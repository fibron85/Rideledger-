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

data class PlatformDaySummary(
    val platformName: String,
    val rideCount: Int,
    val distanceKm: Double,
    val grossFare: Double,
    val tips: Double,
    val totalFuel: Double,
    val totalSalik: Double,
    val totalCommission: Double,
    val totalBookingFee: Double,
    val netIncome: Double
) {
    val totalCosts: Double
        get() = RideCalculator.round2(totalFuel + totalSalik + totalCommission + totalBookingFee)
}

data class DailySummaryItem(
    val dateIso: String,
    val summary: PeriodSummary,
    val platformBreakdowns: List<PlatformDaySummary>
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
            rideCount = rides.size,
            totalTips = RideCalculator.round2(rides.sumOf { it.tips })
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
                RideCalculator.formatDecimal(ride.tips),
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
                    grossAmount = RideCalculator.round2(methodRides.sumOf { it.grossFare + it.tips }),
                    netAmount = RideCalculator.round2(methodRides.sumOf { it.netIncome })
                )
            }
            .sortedByDescending { it.grossAmount }

        val pendingShifts = buildShiftSummaries(pendingWithShift)
        val submittedShifts = buildShiftSummaries(submittedWithShift)

        return FinanceUiState(
            cashOnHandGross = RideCalculator.round2(cashPending.sumOf { it.grossFare + it.tips }),
            cashOnHandNet = RideCalculator.round2(cashPending.sumOf { it.netIncome }),
            cashRideCount = cashPending.size,
            otherPaymentDueGross = RideCalculator.round2(otherPending.sumOf { it.grossFare + it.tips }),
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
                    cashOnHandGross = RideCalculator.round2(cashRides.sumOf { it.grossFare + it.tips }),
                    cashOnHandNet = RideCalculator.round2(cashRides.sumOf { it.netIncome }),
                    otherDueGross = RideCalculator.round2(otherRides.sumOf { it.grossFare + it.tips }),
                    otherDueNet = RideCalculator.round2(otherRides.sumOf { it.netIncome }),
                    totalGross = RideCalculator.round2(dayRides.sumOf { it.grossFare + it.tips }),
                    totalNet = RideCalculator.round2(dayRides.sumOf { it.netIncome }),
                    lastSubmittedAt = dayRides.mapNotNull { it.financeSubmittedAt }.maxOrNull()
                )
            }
    }

    fun buildDailyCycleBreakdown(
        rides: List<RideEntity>,
        settings: AppSettingsEntity,
        cycle: BillingCycle
    ): List<DailySummaryItem> {
        if (rides.isEmpty()) return emptyList()
        return rides
            .map { ride -> ride to RideCalculator.computeShiftDateIso(ride.rideDateMillis, settings) }
            .filter { (_, shiftIso) -> shiftIso >= cycle.startDateIso && shiftIso <= cycle.endDateIso }
            .groupBy { (_, shiftIso) -> shiftIso }
            .entries
            .sortedByDescending { it.key }
            .map { (shiftIso, entries) ->
                val dayRides = entries.map { it.first }
                val platformRows = dayRides
                    .groupBy { it.platformName.ifBlank { "Other" } }
                    .entries
                    .map { (platform, pRides) ->
                        PlatformDaySummary(
                            platformName = platform,
                            rideCount = pRides.size,
                            distanceKm = RideCalculator.round2(pRides.sumOf { it.distanceKm }),
                            grossFare = RideCalculator.round2(pRides.sumOf { it.grossFare }),
                            tips = RideCalculator.round2(pRides.sumOf { it.tips }),
                            totalFuel = RideCalculator.round2(pRides.sumOf { it.fuelCost }),
                            totalSalik = RideCalculator.round2(pRides.sumOf { it.salikCost }),
                            totalCommission = RideCalculator.round2(pRides.sumOf { it.boltCommission }),
                            totalBookingFee = RideCalculator.round2(pRides.sumOf { it.bookingFee }),
                            netIncome = RideCalculator.round2(pRides.sumOf { it.netIncome })
                        )
                    }
                    .sortedByDescending { it.netIncome }

                DailySummaryItem(
                    dateIso = shiftIso,
                    summary = aggregateRides(dayRides),
                    platformBreakdowns = platformRows
                )
            }
    }
}
