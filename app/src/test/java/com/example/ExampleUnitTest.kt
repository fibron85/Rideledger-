package com.example

import com.example.domain.RideCalculator
import com.example.domain.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `bolt commission visibility applies only to Bolt with Street Hail or OTP`() {
        assertTrue(RideCalculator.showsBoltCommission("Bolt", "Street Hail"))
        assertTrue(RideCalculator.showsBoltCommission("bolt", "Bolt OTP"))
        assertTrue(RideCalculator.showsBoltCommission("Bolt", "OTP Booking"))

        assertFalse(RideCalculator.showsBoltCommission("Bolt", "Airport"))
        assertFalse(RideCalculator.showsBoltCommission("DTC", "Street Hail"))
        assertFalse(RideCalculator.showsBoltCommission("Uber", "Bolt OTP"))
    }

    @Test
    fun `booking fee visibility applies to Bolt and DTC only`() {
        assertTrue(RideCalculator.showsBookingFee("Bolt"))
        assertTrue(RideCalculator.showsBookingFee("DTC"))
        assertFalse(RideCalculator.showsBookingFee("Uber"))
        assertFalse(RideCalculator.showsBookingFee("Careem"))
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
}
