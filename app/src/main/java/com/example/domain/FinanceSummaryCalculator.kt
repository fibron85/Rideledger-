package com.example.domain

import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.RideEntity
import com.example.ui.viewmodel.PeriodSummary
import java.util.Locale

data class PaymentMethodDue(
    val method: String,
    val rideCount: Int,
    val grossAmount: Double,
    val netAmount: Double
)

data class FinanceShiftSummary(
    val shiftDateIso: String,
    val rideIds: List<Long>,
    val rideCount: Int,
    val cashOnHandGross: Double,
    val cashOnHandNet: Double,
    val otherDueGross: Double,
    val otherDueNet: Double,
    val totalGross: Double,
    val totalNet: Double,
    val lastSubmittedAt: Long? = null
)

data class FinanceUiState(
    val cashOnHandGross: Double = 0.0,
    val cashOnHandNet: Double = 0.0,
    val cashRideCount: Int = 0,
    val otherPaymentDueGross: Double = 0.0,
    val otherPaymentDueNet: Double = 0.0,
    val otherPaymentRideCount: Int = 0,
    val otherPaymentBreakdown: List<PaymentMethodDue> = emptyList(),
    val pendingShifts: List<FinanceShiftSummary> = emptyList(),
    val submittedShifts: List<FinanceShiftSummary> = emptyList()
) {
    val totalPendingDueGross: Double
        get() = RideCalculator.round2(cashOnHandGross + otherPaymentDueGross)

    val totalPendingDueNet: Double
        get() = RideCalculator.round2(cashOnHandNet + otherPaymentDueNet)

    val allPendingRideIds: List<Long>
        get() = pendingShifts.flatMap { it.rideIds }
}

object FinanceSummaryCalculator {

    fun aggregateRides(rides: List<RideEntity>): PeriodSummary {
        if (rides.isEmpty()) return PeriodSummary()
        return PeriodSummary(
            grossIncome = RideCalculator.round2(rides.sumOf { it.grossFare }),
            netIncome = RideCalculator.round2(rides.sumOf { it.netIncome }),
            totalFuel = RideCalculator.round2(rides.sumOf { it.fuelCost }),
            totalSalik = RideCalculator.round2(rides.sumOf { it.salikCost }),
            totalCommission = RideCalculator.round2(rides.sumOf { it.boltCommission }),
            totalBookingFee = RideCalculator.round2(rides.sumOf { it.bookingFee }),
            totalDistanceKm = RideCalculator.round2(rides.sumOf { it.distanceKm }),
            rideCount = rides.size
        )
    }

    fun isCashPayment(paymentMethod: String): Boolean =
        paymentMethod.trim().equals("Cash", ignoreCase = true)

    fun matchesShiftFilter(
        ride: RideEntity,
        shiftDateIso: String,
        searchQuery: String,
        platformFilter: String?,
        paymentFilter: String?
    ): Boolean {
        if (!platformFilter.isNullOrBlank() &&
            !ride.platformName.equals(platformFilter.trim(), ignoreCase = true)
        ) {
            return false
        }
        val normalizedPayment = ride.paymentMethod.ifBlank { "Cash" }
        if (!paymentFilter.isNullOrBlank() &&
            !normalizedPayment.equals(paymentFilter.trim(), ignoreCase = true)
        ) {
            return false
        }
        val query = searchQuery.trim().lowercase(Locale.US)
        if (query.isNotEmpty()) {
            val haystack = listOf(
                ride.platformName,
                ride.pickupPointName,
                normalizedPayment,
                ride.notes,
                shiftDateIso,
                RideCalculator.formatDecimal(ride.grossFare),
                RideCalculator.formatDecimal(ride.netIncome)
            ).joinToString(" ").lowercase(Locale.US)
            if (!haystack.contains(query)) {
                return false
            }
        }
        return true
    }

    fun computeFinanceState(
        rides: List<RideEntity>,
        settings: AppSettingsEntity
    ): FinanceUiState {
        if (rides.isEmpty()) return FinanceUiState()

        val ridesWithShift = rides.map { ride ->
            ride to RideCalculator.computeShiftDateIso(ride.rideDateMillis, settings)
        }

        val pendingWithShift = ridesWithShift.filter { (ride, _) -> !ride.isSubmittedToFinance }
        val submittedWithShift = ridesWithShift.filter { (ride, _) -> ride.isSubmittedToFinance }

        val pendingRides = pendingWithShift.map { it.first }
        val cashPending = pendingRides.filter { isCashPayment(it.paymentMethod.ifBlank { "Cash" }) }
        val otherPending = pendingRides.filterNot { isCashPayment(it.paymentMethod.ifBlank { "Cash" }) }

        val otherBreakdown = otherPending
            .groupBy { it.paymentMethod.ifBlank { "Other" } }
            .entries
            .map { (method, methodRides) ->
                PaymentMethodDue(
                    method = method,
                    rideCount = methodRides.size,
                    grossAmount = RideCalculator.round2(methodRides.sumOf { it.grossFare }),
                    netAmount = RideCalculator.round2(methodRides.sumOf { it.netIncome })
                )
            }
            .sortedByDescending { it.grossAmount }

        val pendingShifts = buildShiftSummaries(pendingWithShift)
        val submittedShifts = buildShiftSummaries(submittedWithShift)

        return FinanceUiState(
            cashOnHandGross = RideCalculator.round2(cashPending.sumOf { it.grossFare }),
            cashOnHandNet = RideCalculator.round2(cashPending.sumOf { it.netIncome }),
            cashRideCount = cashPending.size,
            otherPaymentDueGross = RideCalculator.round2(otherPending.sumOf { it.grossFare }),
            otherPaymentDueNet = RideCalculator.round2(otherPending.sumOf { it.netIncome }),
            otherPaymentRideCount = otherPending.size,
            otherPaymentBreakdown = otherBreakdown,
            pendingShifts = pendingShifts,
            submittedShifts = submittedShifts
        )
    }

    private fun buildShiftSummaries(
        ridesWithShift: List<Pair<RideEntity, String>>
    ): List<FinanceShiftSummary> {
        return ridesWithShift
            .groupBy { (_, shiftIso) -> shiftIso }
            .entries
            .sortedByDescending { it.key }
            .map { (shiftIso, entries) ->
                val dayRides = entries.map { it.first }
                val cashRides = dayRides.filter { isCashPayment(it.paymentMethod.ifBlank { "Cash" }) }
                val otherRides = dayRides.filterNot { isCashPayment(it.paymentMethod.ifBlank { "Cash" }) }
                FinanceShiftSummary(
                    shiftDateIso = shiftIso,
                    rideIds = dayRides.map { it.id },
                    rideCount = dayRides.size,
                    cashOnHandGross = RideCalculator.round2(cashRides.sumOf { it.grossFare }),
                    cashOnHandNet = RideCalculator.round2(cashRides.sumOf { it.netIncome }),
                    otherDueGross = RideCalculator.round2(otherRides.sumOf { it.grossFare }),
                    otherDueNet = RideCalculator.round2(otherRides.sumOf { it.netIncome }),
                    totalGross = RideCalculator.round2(dayRides.sumOf { it.grossFare }),
                    totalNet = RideCalculator.round2(dayRides.sumOf { it.netIncome }),
                    lastSubmittedAt = dayRides.mapNotNull { it.financeSubmittedAt }.maxOrNull()
                )
            }
    }
}
