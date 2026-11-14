package com.example.domain

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object RideCalculator {

    fun round2(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return (value * 100.0).roundToInt() / 100.0
    }

    fun isBoltPlatform(platformName: String): Boolean =
        platformName.trim().equals("bolt", ignoreCase = true)

    fun isDtcPlatform(platformName: String): Boolean =
        platformName.trim().equals("dtc", ignoreCase = true)

    fun showsBookingFee(platformName: String): Boolean =
        isBoltPlatform(platformName) || isDtcPlatform(platformName)

    fun isCommissionEligiblePickup(pickupName: String): Boolean {
        val normalized = pickupName.trim().lowercase(Locale.US)
        if (normalized.isEmpty()) return false
        return normalized == "street hail" ||
            normalized == "bolt otp" ||
            normalized == "otp booking" ||
            normalized == "bolt otp booking" ||
            normalized.contains("street hail") ||
            normalized.contains("otp")
    }

    fun showsBoltCommission(platformName: String, pickupName: String): Boolean =
        isBoltPlatform(platformName) && isCommissionEligiblePickup(pickupName)

    fun calculateFuelCost(distanceKm: Double, fuelCostPerKm: Double): Double =
        round2(max(0.0, distanceKm) * max(0.0, fuelCostPerKm))

    fun calculateNetIncome(
        grossFare: Double,
        fuelCost: Double,
        salikCost: Double,
        boltCommission: Double,
        bookingFee: Double
    ): Double = round2(grossFare - fuelCost - salikCost - boltCommission - bookingFee)

    fun formatMoney(amount: Double, currencyCode: String): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currencyCode ${nf.format(round2(amount))}"
    }

    fun formatDecimal(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return nf.format(round2(amount))
    }

    fun toIsoDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun toMonthPrefix(year: Int, monthZeroBased: Int): String =
        String.format(Locale.US, "%04d-%02d", year, monthZeroBased + 1)

    fun currentIsoDate(): String = toIsoDate(System.currentTimeMillis())

    fun currentMonthPrefix(): String {
        val cal = Calendar.getInstance()
        return toMonthPrefix(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
            val date = parser.parse(isoDate) ?: return isoDate
            formatter.format(date)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatMonthLabel(monthPrefix: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
            val date = parser.parse(monthPrefix) ?: return monthPrefix
            formatter.format(date)
        } catch (_: Exception) {
            monthPrefix
        }
    }
}
