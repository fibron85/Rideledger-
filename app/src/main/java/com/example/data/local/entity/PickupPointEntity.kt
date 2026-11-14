package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pickup_points",
    indices = [Index(value = ["name"], unique = true)]
)
data class PickupPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val boltBookingFee: Double = 0.0,
    val dtcBookingFee: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
