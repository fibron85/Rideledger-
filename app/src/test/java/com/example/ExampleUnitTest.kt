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
    }

    @Test
    fun `fuel cost and net income calculation is accurate`() {
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
            fuelCost = 6.53, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0, netIncome = 85.47,
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
            summary = PeriodSummary(442.0, 435.47, 6.53, 0.0, 0.0, 0.0, 14.5, 2),
            dailyGroups = listOf(
                DailyReportGroup(
                    dateIso = "2026-11-12",
                    summary = PeriodSummary(442.0, 435.47, 6.53, 0.0, 0.0, 0.0, 14.5, 2),
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
        assertEquals(85.47, parsedUber.netIncome, 0.001)
        assertEquals("Airport drop", parsedUber.notes)
        assertFalse(parsedUber.isSubmittedToFinance)

        val parsedEvent = parsed[1]
        assertEquals("DTC Event Booking", parsedEvent.platformName)
        assertEquals("Account", parsedEvent.paymentMethod)
        assertEquals(350.0, parsedEvent.grossFare, 0.001)
        assertEquals(350.0, parsedEvent.netIncome, 0.001)
        assertEquals("Exhibition shuttle", parsedEvent.notes)
        assertTrue(parsedEvent.isSubmittedToFinance)
    }
}
