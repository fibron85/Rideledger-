package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rides",
    indices = [
        Index(value = ["driverId", "rideDateIso"]),
        Index(value = ["driverId"])
    ]
)
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val driverId: String,
    val rideDateMillis: Long,
    val rideDateIso: String,
    val platformName: String,
    val pickupPointName: String,
    val distanceKm: Double,
    val grossFare: Double,
    val fuelCostPerKmApplied: Double,
    val fuelCost: Double,
    val salikCost: Double,
    val boltCommission: Double,
    val bookingFee: Double,
    val netIncome: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
