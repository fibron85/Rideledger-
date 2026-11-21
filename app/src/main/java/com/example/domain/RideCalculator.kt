package com.example.domain

import com.example.data.local.entity.AppSettingsEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class BillingCycle(
    val startDateIso: String,
    val endDateIso: String,
    val label: String
)

object RideCalculator {

    val PAYMENT_METHODS = listOf("Card", "Cash", "InAppPaid", "Account")
    val STANDARD_PAYMENT_METHODS = listOf("Card", "Cash", "InAppPaid")
    val DEFAULT_PLATFORM_NAMES = listOf("Bolt", "DTC", "Uber", "DTC Event Booking")

    fun normalizePlatformInput(name: String): String =
        name.trim().replace(Regex("\\s+"), " ")

    fun isDefaultPlatform(platformName: String): Boolean {
        val normalized = normalizePlatformInput(platformName).lowercase(Locale.US)
        return normalized == "bolt" ||
            normalized == "dtc" ||
            normalized == "uber" ||
            normalized == "dtc event booking"
    }

    fun round2(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return (value * 100.0).roundToInt() / 100.0
    }

    fun isBoltPlatform(platformName: String): Boolean =
        platformName.trim().equals("bolt", ignoreCase = true)

    fun isDtcPlatform(platformName: String): Boolean =
        platformName.trim().equals("dtc", ignoreCase = true)

    fun isDtcEventBookingPlatform(platformName: String): Boolean =
        platformName.trim().equals("dtc event booking", ignoreCase = true)

    fun isUberPlatform(platformName: String): Boolean =
        platformName.trim().equals("uber", ignoreCase = true)

    fun isCareemPlatform(platformName: String): Boolean =
        platformName.trim().equals("careem", ignoreCase = true)

    fun usesYourEarningLabel(platformName: String): Boolean =
        isUberPlatform(platformName) || isCareemPlatform(platformName)

    fun isDtcEventBooking(platformName: String, pickupName: String = ""): Boolean {
        if (isDtcEventBookingPlatform(platformName)) return true
        if (!isDtcPlatform(platformName)) return false
        val normalized = pickupName.trim().lowercase(Locale.US)
        return normalized == "dtc event booking" || normalized == "event booking"
    }

    fun allowedPaymentMethods(platformName: String): List<String> =
        if (isDtcEventBookingPlatform(platformName)) listOf("Account") else STANDARD_PAYMENT_METHODS

    fun showsDistanceAndFuel(platformName: String, pickupName: String = ""): Boolean =
        !isDtcEventBooking(platformName, pickupName)

    fun showsSalikCost(platformName: String, pickupName: String = ""): Boolean =
        !isDtcEventBooking(platformName, pickupName) && !isUberPlatform(platformName)

    fun showsTips(platformName: String, pickupName: String = ""): Boolean =
        !isDtcEventBooking(platformName, pickupName) &&
            (isBoltPlatform(platformName) || isDtcPlatform(platformName) || isUberPlatform(platformName))

    fun showsPickupPoint(platformName: String): Boolean =
        isBoltPlatform(platformName) || isDtcPlatform(platformName)

    fun isPickupPointAllowedForPlatform(platformName: String, pickupName: String): Boolean {
        val cleanPickup = pickupName.trim().lowercase(Locale.US)
        if (cleanPickup.isEmpty()) return false
        return when {
            isBoltPlatform(platformName) -> !cleanPickup.contains("dtc")
            isDtcPlatform(platformName) -> !cleanPickup.contains("bolt")
            else -> false
        }
    }

    fun showsBookingFee(platformName: String, pickupName: String = ""): Boolean =
        (isBoltPlatform(platformName) || isDtcPlatform(platformName)) &&
            !isDtcEventBooking(platformName, pickupName)

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
        bookingFee: Double,
        tips: Double = 0.0
    ): Double = round2(grossFare + max(0.0, tips) - fuelCost - salikCost - boltCommission - bookingFee)

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

    fun toTimeLabel(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun toTimeLabel(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return toTimeLabel(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun formatShiftWindow(settings: AppSettingsEntity): String =
        "${toTimeLabel(settings.shiftStartHour, settings.shiftStartMinute)} – ${toTimeLabel(settings.shiftEndHour, settings.shiftEndMinute)}"

    fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
        val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        dateCal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        dateCal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        dateCal.set(Calendar.SECOND, 0)
        dateCal.set(Calendar.MILLISECOND, 0)
        return dateCal.timeInMillis
    }

    fun isTimeWithinShift(hour: Int, minute: Int, settings: AppSettingsEntity): Boolean {
        val t = (hour.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
        val start = settings.shiftStartTotalMinutes
        val end = settings.shiftEndTotalMinutes
        return if (start <= end) {
            t in start..end
        } else {
            // Overnight shift (e.g., 18:00 to 06:00)
            t >= start || t <= end
        }
    }

    /**
     * Determines which shift day ("yyyy-MM-dd") a ride timestamp belongs to.
     * For overnight shifts (start > end, e.g. 18:00 -> 06:00), rides occurring on day D
     * before shiftStart (e.g. 00:00..06:00 or before shiftStart) are attributed to day D-1's shift.
     */
    fun computeShiftDateIso(timestampMillis: Long, settings: AppSettingsEntity): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val t = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = settings.shiftStartTotalMinutes
        val end = settings.shiftEndTotalMinutes

        if (start > end && t < start) {
            // If overnight shift (e.g., 18:00 to 06:00) and time is in early morning (<= end or before start),
            // attribute to the shift that started the previous calendar day when within/near overnight window.
            if (t <= end) {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
        }
        return toIsoDate(cal.timeInMillis)
    }

    fun currentShiftIsoDate(settings: AppSettingsEntity): String =
        computeShiftDateIso(System.currentTimeMillis(), settings)

    fun currentIsoDate(): String = toIsoDate(System.currentTimeMillis())

    /**
     * Calculates the custom billing month running from Day 21 of Month M to Day 20 of Month M+1.
     */
    fun getBillingCycleForDate(isoDate: String): BillingCycle {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = try {
            parser.parse(isoDate)
        } catch (_: Exception) {
            null
        } ?: Date()

        val cal = Calendar.getInstance().apply { time = parsed }
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val startCal = (cal.clone() as Calendar).apply {
            if (day < 21) {
                add(Calendar.MONTH, -1)
            }
            set(Calendar.DAY_OF_MONTH, 21)
        }

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 20)
        }

        val startIso = toIsoDate(startCal.timeInMillis)
        val endIso = toIsoDate(endCal.timeInMillis)
        return BillingCycle(
            startDateIso = startIso,
            endDateIso = endIso,
            label = formatBillingCycleLabel(startCal.time, endCal.time)
        )
    }

    fun shiftBillingCycle(currentStartIso: String, deltaMonths: Int): BillingCycle {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try {
            parser.parse(currentStartIso)
        } catch (_: Exception) {
            null
        } ?: Date()

        val startCal = Calendar.getInstance().apply {
            time = date
            add(Calendar.MONTH, deltaMonths)
            set(Calendar.DAY_OF_MONTH, 21)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 20)
        }
        return BillingCycle(
            startDateIso = toIsoDate(startCal.timeInMillis),
            endDateIso = toIsoDate(endCal.timeInMillis),
            label = formatBillingCycleLabel(startCal.time, endCal.time)
        )
    }

    private fun formatBillingCycleLabel(startDate: Date, endDate: Date): String {
        val startFmt = SimpleDateFormat("dd MMM", Locale.US)
        val endFmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return "${startFmt.format(startDate)} – ${endFmt.format(endDate)}"
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
}
