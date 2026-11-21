package com.example

import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.RideCalculator
import com.example.domain.SecurityUtils
import com.example.ui.viewmodel.DailyReportGroup
import com.example.ui.viewmodel.PeriodSummary
import com.example.util.ReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun `driver id and password validation enforces 7 and 6 digits`() {
        assertTrue(SecurityUtils.isValidDriverId("1234567"))
        assertFalse(SecurityUtils.isValidDriverId("123456"))
        assertFalse(SecurityUtils.isValidDriverId("123456a"))

        assertTrue(SecurityUtils.isValidPassword("654321"))
        assertFalse(SecurityUtils.isValidPassword("12345"))
        assertFalse(SecurityUtils.isValidPassword("1234567"))
    }

    @Test
    fun `pickup point visibility applies only to Bolt and DTC`() {
        assertTrue(RideCalculator.showsPickupPoint("Bolt"))
        assertTrue(RideCalculator.showsPickupPoint("dtc"))
        assertFalse(RideCalculator.showsPickupPoint("Uber"))
        assertFalse(RideCalculator.showsPickupPoint("Careem"))
    }

    @Test
    fun `bolt hides dtc pickup points and dtc hides bolt pickup points`() {
        assertTrue(RideCalculator.isPickupPointAllowedForPlatform("Bolt", "Street Hail"))
        assertTrue(RideCalculator.isPickupPointAllowedForPlatform("Bolt", "Bolt OTP"))
        assertTrue(RideCalculator.isPickupPointAllowedForPlatform("Bolt", "Airport"))
        assertFalse(RideCalculator.isPickupPointAllowedForPlatform("Bolt", "DTC Event Booking"))
        assertFalse(RideCalculator.isPickupPointAllowedForPlatform("Bolt", "DTC"))

        assertTrue(RideCalculator.isPickupPointAllowedForPlatform("DTC", "Street Hail"))
        assertTrue(RideCalculator.isPickupPointAllowedForPlatform("DTC", "DTC Event Booking"))
        assertTrue(RideCalculator.isPickupPointAllowedForPlatform("DTC", "Airport"))
        assertFalse(RideCalculator.isPickupPointAllowedForPlatform("DTC", "Bolt OTP"))
        assertFalse(RideCalculator.isPickupPointAllowedForPlatform("DTC", "Bolt"))
    }

    @Test
    fun `bolt commission visibility applies only to Bolt with Street Hail or OTP`() {
        assertTrue(RideCalculator.showsBoltCommission("Bolt", "Street Hail"))
        assertTrue(RideCalculator.showsBoltCommission("bolt", "Bolt OTP"))
        assertTrue(RideCalculator.showsBoltCommission("Bolt", "OTP Booking"))

        assertFalse(RideCalculator.showsBoltCommission("Bolt", "Airport"))
        assertFalse(RideCalculator.showsBoltCommission("DTC", "Street Hail"))
        assertFalse(RideCalculator.showsBoltCommission("Uber", "Bolt OTP"))
    }

    @Test
    fun `booking fee visibility applies to Bolt and DTC except DTC Event Booking`() {
        assertTrue(RideCalculator.showsBookingFee("Bolt", "Airport"))
        assertTrue(RideCalculator.showsBookingFee("DTC", "Airport"))
        assertFalse(RideCalculator.showsBookingFee("DTC", "DTC Event Booking"))
        assertFalse(RideCalculator.showsBookingFee("Uber", "Airport"))
        assertFalse(RideCalculator.showsBookingFee("Careem", "City Center"))
    }

    @Test
    fun `dtc event booking platform rules, account payment method, and uber salik visibility are enforced`() {
        assertTrue(RideCalculator.isDtcEventBookingPlatform("DTC Event Booking"))
        assertTrue(RideCalculator.isDtcEventBooking("DTC Event Booking"))
        assertFalse(RideCalculator.showsPickupPoint("DTC Event Booking"))
        assertFalse(RideCalculator.showsDistanceAndFuel("DTC Event Booking"))
        assertFalse(RideCalculator.showsSalikCost("DTC Event Booking"))
        assertFalse(RideCalculator.showsBookingFee("DTC Event Booking"))

        // Account payment method ONLY visible for DTC Event Booking
        assertEquals(listOf("Account"), RideCalculator.allowedPaymentMethods("DTC Event Booking"))
        assertEquals(listOf("Card", "Cash", "InAppPaid"), RideCalculator.allowedPaymentMethods("Bolt"))
        assertEquals(listOf("Card", "Cash", "InAppPaid"), RideCalculator.allowedPaymentMethods("DTC"))
        assertEquals(listOf("Card", "Cash", "InAppPaid"), RideCalculator.allowedPaymentMethods("Uber"))
        assertEquals(listOf("Card", "Cash", "InAppPaid"), RideCalculator.allowedPaymentMethods("Careem"))

        // Uber & Careem use "Your Earning" label
        assertTrue(RideCalculator.usesYourEarningLabel("Uber"))
        assertTrue(RideCalculator.usesYourEarningLabel("Careem"))
        assertFalse(RideCalculator.usesYourEarningLabel("Bolt"))
        assertFalse(RideCalculator.usesYourEarningLabel("DTC"))
        assertFalse(RideCalculator.usesYourEarningLabel("DTC Event Booking"))

        // Uber hides Salik cost while keeping distance and fuel
        assertTrue(RideCalculator.showsDistanceAndFuel("Uber"))
        assertFalse(RideCalculator.showsSalikCost("Uber"))
        assertTrue(RideCalculator.showsSalikCost("Bolt"))
        assertTrue(RideCalculator.showsSalikCost("DTC"))
        assertTrue(RideCalculator.showsSalikCost("Careem"))

        // Tips supported for Bolt, DTC, Uber; hidden for Careem & DTC Event Booking
        assertTrue(RideCalculator.showsTips("Bolt"))
        assertTrue(RideCalculator.showsTips("DTC"))
        assertTrue(RideCalculator.showsTips("Uber"))
        assertFalse(RideCalculator.showsTips("Careem"))
        assertFalse(RideCalculator.showsTips("DTC Event Booking"))
    }

    @Test
    fun `fuel cost and net income calculation is accurate with and without tips`() {
        val fuel = RideCalculator.calculateFuelCost(distanceKm = 20.5, fuelCostPerKm = 0.45)
        assertEquals(9.23, fuel, 0.001)

        val net = RideCalculator.calculateNetIncome(
            grossFare = 120.0,
            fuelCost = fuel,
            salikCost = 8.0,
            boltCommission = 15.0,
            bookingFee = 5.0
        )
        assertEquals(82.77, net, 0.001)

        val netWithTips = RideCalculator.calculateNetIncome(
            grossFare = 120.0,
            fuelCost = fuel,
            salikCost = 8.0,
            boltCommission = 15.0,
            bookingFee = 5.0,
            tips = 12.5
        )
        assertEquals(95.27, netWithTips, 0.001)
    }

    @Test
    fun `overnight shift attributes early morning rides to previous shift day`() {
        val nightShift = AppSettingsEntity(
            shiftStartHour = 18,
            shiftStartMinute = 0,
            shiftEndHour = 6,
            shiftEndMinute = 0
        )

        val eveningCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 21, 30, 0)
        }
        assertEquals("2026-11-17", RideCalculator.computeShiftDateIso(eveningCal.timeInMillis, nightShift))

        val afterMidnightCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 2, 15, 0)
        }
        assertEquals("2026-11-17", RideCalculator.computeShiftDateIso(afterMidnightCal.timeInMillis, nightShift))
    }

    @Test
    fun `billing month runs from day 21 to next month day 20`() {
        val midNov = RideCalculator.getBillingCycleForDate("2026-11-17")
        assertEquals("2026-10-21", midNov.startDateIso)
        assertEquals("2026-11-20", midNov.endDateIso)

        val twentieth = RideCalculator.getBillingCycleForDate("2026-11-20")
        assertEquals("2026-10-21", twentieth.startDateIso)
        assertEquals("2026-11-20", twentieth.endDateIso)

        val twentyFirst = RideCalculator.getBillingCycleForDate("2026-11-21")
        assertEquals("2026-11-21", twentyFirst.startDateIso)
        assertEquals("2026-12-20", twentyFirst.endDateIso)

        val decEnd = RideCalculator.getBillingCycleForDate("2026-12-25")
        assertEquals("2026-12-21", decEnd.startDateIso)
        assertEquals("2027-01-20", decEnd.endDateIso)
    }

    @Test
    fun `xlsx exporter generates valid zip OpenXML bytes`() {
        val ride = RideEntity(
            id = 1,
            driverId = "1234567",
            rideDateMillis = System.currentTimeMillis(),
            rideDateIso = "2026-11-17",
            platformName = "Bolt",
            pickupPointName = "Street Hail",
            distanceKm = 15.0,
            grossFare = 80.0,
            fuelCostPerKmApplied = 0.45,
            fuelCost = 6.75,
            salikCost = 4.0,
            boltCommission = 10.0,
            bookingFee = 2.0,
            netIncome = 57.25
        )
        val bytes = ReportExporter.buildXlsxBytes(
            driver = DriverAccountEntity("1234567", "hash", "Tariq", "0501234567"),
            settings = AppSettingsEntity(),
            periodLabel = "21 Oct – 20 Nov 2026",
            summary = PeriodSummary(80.0, 57.25, 6.75, 4.0, 10.0, 2.0, 15.0, 1),
            dailyGroups = listOf(DailyReportGroup("2026-11-17", PeriodSummary(80.0, 57.25, 6.75, 4.0, 10.0, 2.0, 15.0, 1), listOf(ride)))
        )
        assertTrue(bytes.size > 200)
        // ZIP header 'P' 'K'
        assertEquals(0x50.toByte(), bytes[0])
        assertEquals(0x4B.toByte(), bytes[1])
    }

    @Test
    fun `finance summary separates cash on hand from other due and clears submitted rides`() {
        val ts = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 14, 0, 0)
        }.timeInMillis
        val settings = AppSettingsEntity()

        val cashPending = RideEntity(
            id = 1, driverId = "1234567", rideDateMillis = ts, rideDateIso = "2026-11-17",
            platformName = "Bolt", pickupPointName = "Street Hail", paymentMethod = "Cash",
            distanceKm = 10.0, grossFare = 100.0, fuelCostPerKmApplied = 0.45,
            fuelCost = 4.5, salikCost = 0.0, boltCommission = 10.0, bookingFee = 0.0, netIncome = 85.5,
            isSubmittedToFinance = false
        )
        val cardPending = cashPending.copy(
            id = 2, platformName = "DTC", pickupPointName = "Airport", paymentMethod = "Card",
            grossFare = 150.0, netIncome = 120.5, isSubmittedToFinance = false
        )
        val accountPending = cashPending.copy(
            id = 3, platformName = "Uber", pickupPointName = "", paymentMethod = "Account",
            grossFare = 75.0, netIncome = 65.0, isSubmittedToFinance = false
        )
        val alreadySubmittedCash = cashPending.copy(
            id = 4, paymentMethod = "Cash", grossFare = 200.0, netIncome = 180.0,
            isSubmittedToFinance = true, financeSubmittedAt = ts
        )

        val state = com.example.domain.FinanceSummaryCalculator.computeFinanceState(
            listOf(cashPending, cardPending, accountPending, alreadySubmittedCash),
            settings
        )

        assertEquals(100.0, state.cashOnHandGross, 0.001)
        assertEquals(1, state.cashRideCount)
        assertEquals(225.0, state.otherPaymentDueGross, 0.001)
        assertEquals(2, state.otherPaymentRideCount)
        assertEquals(2, state.otherPaymentBreakdown.size)
        assertEquals(1, state.pendingShifts.size)
        assertEquals(1, state.submittedShifts.size)
    }

    @Test
    fun `daily shift filter matches platform payment method and search query`() {
        val ride = RideEntity(
            id = 10, driverId = "1234567", rideDateMillis = System.currentTimeMillis(), rideDateIso = "2026-11-17",
            platformName = "DTC", pickupPointName = "DTC Event Booking", paymentMethod = "InAppPaid",
            distanceKm = 0.0, grossFare = 300.0, fuelCostPerKmApplied = 0.0,
            fuelCost = 0.0, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0, netIncome = 300.0,
            notes = "VIP concert"
        )
        assertTrue(com.example.domain.FinanceSummaryCalculator.matchesShiftFilter(ride, "2026-11-17", "", "DTC", "InAppPaid"))
        assertTrue(com.example.domain.FinanceSummaryCalculator.matchesShiftFilter(ride, "2026-11-17", "concert", null, null))
        assertFalse(com.example.domain.FinanceSummaryCalculator.matchesShiftFilter(ride, "2026-11-17", "", "Bolt", null))
        assertFalse(com.example.domain.FinanceSummaryCalculator.matchesShiftFilter(ride, "2026-11-17", "", null, "Cash"))
    }

    @Test
    fun `xlsx backup export and import roundtrip preserves rides notes and finance status`() {
        val ts1 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12, 10, 15, 0) }.timeInMillis
        val ts2 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12, 18, 45, 0) }.timeInMillis
        val settings = AppSettingsEntity()

        val uberRide = RideEntity(
            id = 1, driverId = "1234567", rideDateMillis = ts1, rideDateIso = "2026-11-12",
            platformName = "Uber", pickupPointName = "", paymentMethod = "Cash",
            distanceKm = 14.5, grossFare = 92.0, fuelCostPerKmApplied = 0.45,
            fuelCost = 6.53, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0,
            netIncome = 95.47, tips = 10.0,
            notes = "Airport drop", isSubmittedToFinance = false
        )
        val eventRide = RideEntity(
            id = 2, driverId = "1234567", rideDateMillis = ts2, rideDateIso = "2026-11-12",
            platformName = "DTC Event Booking", pickupPointName = "", paymentMethod = "Account",
            distanceKm = 0.0, grossFare = 350.0, fuelCostPerKmApplied = 0.0,
            fuelCost = 0.0, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0, netIncome = 350.0,
            notes = "Exhibition shuttle", isSubmittedToFinance = true, financeSubmittedAt = ts2
        )

        val bytes = ReportExporter.buildXlsxBytes(
            driver = DriverAccountEntity("1234567", "hash", "Tariq", "0501234567"),
            settings = settings,
            periodLabel = "21 Oct – 20 Nov 2026",
            summary = PeriodSummary(442.0, 445.47, 6.53, 0.0, 0.0, 0.0, 14.5, 2, 10.0),
            dailyGroups = listOf(
                DailyReportGroup(
                    dateIso = "2026-11-12",
                    summary = PeriodSummary(442.0, 445.47, 6.53, 0.0, 0.0, 0.0, 14.5, 2, 10.0),
                    rides = listOf(uberRide, eventRide)
                )
            )
        )

        val parsed = ReportExporter.parseXlsxBytes(bytes, "1234567", settings)
        assertEquals(2, parsed.size)

        val parsedUber = parsed[0]
        assertEquals("Uber", parsedUber.platformName)
        assertEquals("Cash", parsedUber.paymentMethod)
        assertEquals(92.0, parsedUber.grossFare, 0.001)
        assertEquals(10.0, parsedUber.tips, 0.001)
        assertEquals(95.47, parsedUber.netIncome, 0.001)
        assertEquals("Airport drop", parsedUber.notes)
        assertFalse(parsedUber.isSubmittedToFinance)

        val parsedEvent = parsed[1]
        assertEquals("DTC Event Booking", parsedEvent.platformName)
        assertEquals("Account", parsedEvent.paymentMethod)
        assertEquals(350.0, parsedEvent.grossFare, 0.001)
        assertEquals(0.0, parsedEvent.tips, 0.001)
        assertEquals(350.0, parsedEvent.netIncome, 0.001)
        assertEquals("Exhibition shuttle", parsedEvent.notes)
        assertTrue(parsedEvent.isSubmittedToFinance)
    }

    @Test
    fun `daily cycle breakdown groups rides by shift day and platform separately`() {
        val settings = AppSettingsEntity(
            shiftStartHour = 18,
            shiftStartMinute = 0,
            shiftEndHour = 6,
            shiftEndMinute = 0
        )
        val cycle = RideCalculator.getBillingCycleForDate("2026-11-17") // 2026-10-21 .. 2026-11-20

        val boltEvening = RideEntity(
            id = 1, driverId = "1234567",
            rideDateMillis = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 17, 20, 0, 0) }.timeInMillis,
            rideDateIso = "2026-11-17",
            platformName = "Bolt", pickupPointName = "Street Hail", paymentMethod = "Cash",
            distanceKm = 12.0, grossFare = 90.0, fuelCostPerKmApplied = 0.45,
            fuelCost = 5.4, salikCost = 4.0, boltCommission = 10.0, bookingFee = 0.0,
            netIncome = 75.6, tips = 5.0
        )
        val uberLateNightSameShift = RideEntity(
            id = 2, driverId = "1234567",
            rideDateMillis = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 18, 1, 30, 0) }.timeInMillis,
            rideDateIso = "2026-11-18",
            platformName = "Uber", pickupPointName = "", paymentMethod = "Card",
            distanceKm = 20.0, grossFare = 140.0, fuelCostPerKmApplied = 0.45,
            fuelCost = 9.0, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0,
            netIncome = 141.0, tips = 10.0
        )
        val dtcEventEarlierDay = RideEntity(
            id = 3, driverId = "1234567",
            rideDateMillis = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 15, 22, 0, 0) }.timeInMillis,
            rideDateIso = "2026-11-15",
            platformName = "DTC Event Booking", pickupPointName = "", paymentMethod = "Account",
            distanceKm = 0.0, grossFare = 300.0, fuelCostPerKmApplied = 0.0,
            fuelCost = 0.0, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0,
            netIncome = 300.0, tips = 0.0
        )
        val outOfCycleRide = dtcEventEarlierDay.copy(
            id = 4,
            rideDateMillis = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 12, 0, 0) }.timeInMillis,
            rideDateIso = "2026-10-20"
        )

        val days = com.example.domain.FinanceSummaryCalculator.buildDailyCycleBreakdown(
            rides = listOf(boltEvening, uberLateNightSameShift, dtcEventEarlierDay, outOfCycleRide),
            settings = settings,
            cycle = cycle
        )

        // Only 2 days inside 21 Oct .. 20 Nov, newest first
        assertEquals(2, days.size)
        assertEquals("2026-11-17", days[0].dateIso)
        assertEquals("2026-11-15", days[1].dateIso)

        val nov17 = days[0]
        assertEquals(2, nov17.summary.rideCount)
        assertEquals(230.0, nov17.summary.grossIncome, 0.001)
        assertEquals(15.0, nov17.summary.totalTips, 0.001)
        assertEquals(216.6, nov17.summary.netIncome, 0.001)
        assertEquals(2, nov17.platformBreakdowns.size)
        // Sorted by net income descending -> Uber (141.0) then Bolt (75.6)
        assertEquals("Uber", nov17.platformBreakdowns[0].platformName)
        assertEquals(140.0, nov17.platformBreakdowns[0].grossFare, 0.001)
        assertEquals(10.0, nov17.platformBreakdowns[0].tips, 0.001)
        assertEquals("Bolt", nov17.platformBreakdowns[1].platformName)
        assertEquals(90.0, nov17.platformBreakdowns[1].grossFare, 0.001)
        assertEquals(5.0, nov17.platformBreakdowns[1].tips, 0.001)

        val nov15 = days[1]
        assertEquals(1, nov15.summary.rideCount)
        assertEquals(300.0, nov15.summary.netIncome, 0.001)
        assertEquals("DTC Event Booking", nov15.platformBreakdowns[0].platformName)
    }

    @Test
    fun `default platforms are Bolt DTC Uber DTC Event Booking and normalization collapses spaces`() {
        assertTrue(RideCalculator.isDefaultPlatform("Bolt"))
        assertTrue(RideCalculator.isDefaultPlatform("BOLT"))
        assertTrue(RideCalculator.isDefaultPlatform("dtc"))
        assertTrue(RideCalculator.isDefaultPlatform("UBER"))
        assertTrue(RideCalculator.isDefaultPlatform("  dtc   event   booking "))
        assertFalse(RideCalculator.isDefaultPlatform("Careem"))
        assertFalse(RideCalculator.isDefaultPlatform("Yango"))
        assertEquals("DTC Event Booking", RideCalculator.normalizePlatformInput("  DTC   Event   Booking "))
    }

    @Test
    fun `daily reminder scheduler computes 4 PM and 4 AM trigger times and active windows`() {
        // 10:30 AM -> same day 16:00, next day 04:00
        val morningNow = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 10, 30, 15)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val next4Pm = com.example.util.RideReminderScheduler.nextDailyTriggerMillis(morningNow, hourOfDay = 16, minute = 0)
        val cal4Pm = Calendar.getInstance().apply { timeInMillis = next4Pm }
        assertEquals(18, cal4Pm.get(Calendar.DAY_OF_MONTH))
        assertEquals(16, cal4Pm.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal4Pm.get(Calendar.MINUTE))

        val next4Am = com.example.util.RideReminderScheduler.nextDailyTriggerMillis(morningNow, hourOfDay = 4, minute = 0)
        val cal4Am = Calendar.getInstance().apply { timeInMillis = next4Am }
        assertEquals(19, cal4Am.get(Calendar.DAY_OF_MONTH))
        assertEquals(4, cal4Am.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal4Am.get(Calendar.MINUTE))

        // At 4:15 PM -> evening in-app reminder active
        val at415Pm = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 16, 15, 0)
        }.timeInMillis
        val eveningPrompt = com.example.util.RideReminderScheduler.activeInAppReminder(at415Pm)
        assertEquals("2026-11-18_16", eveningPrompt?.slotKey)

        // At 4:15 AM -> night-shift wrap-up in-app reminder active
        val at415Am = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 4, 15, 0)
        }.timeInMillis
        val morningPrompt = com.example.util.RideReminderScheduler.activeInAppReminder(at415Am)
        assertEquals("2026-11-18_04", morningPrompt?.slotKey)

        // At 10:30 AM -> outside reminder windows
        assertEquals(null, com.example.util.RideReminderScheduler.activeInAppReminder(morningNow))

        // Finance reminder at 4:15 PM & 4:15 AM only when pendingRideCount > 0
        val emptyFinance = com.example.domain.FinanceUiState()
        assertEquals(
            null,
            com.example.util.RideReminderScheduler.activeFinanceInAppReminder(at415Pm, emptyFinance, "AED")
        )

        val pendingFinance = com.example.domain.FinanceUiState(
            cashOnHandGross = 120.0,
            cashOnHandNet = 108.0,
            cashRideCount = 1,
            otherPaymentDueGross = 80.0,
            otherPaymentDueNet = 72.0,
            otherPaymentRideCount = 1,
            pendingShifts = listOf(
                com.example.domain.FinanceShiftSummary(
                    shiftDateIso = "2026-11-18",
                    rideIds = listOf(1L, 2L),
                    rideCount = 2,
                    cashOnHandGross = 120.0,
                    cashOnHandNet = 108.0,
                    otherDueGross = 80.0,
                    otherDueNet = 72.0,
                    totalGross = 200.0,
                    totalNet = 180.0
                )
            )
        )
        val finEvening = com.example.util.RideReminderScheduler.activeFinanceInAppReminder(at415Pm, pendingFinance, "AED")
        assertEquals("2026-11-18_16_fin", finEvening?.slotKey)
        assertTrue(finEvening?.message?.contains("200.00") == true)

        val finMorning = com.example.util.RideReminderScheduler.activeFinanceInAppReminder(at415Am, pendingFinance, "AED")
        assertEquals("2026-11-18_04_fin", finMorning?.slotKey)
    }
}
