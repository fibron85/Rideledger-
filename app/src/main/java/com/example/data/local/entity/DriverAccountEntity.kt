package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_accounts")
data class DriverAccountEntity(
    @PrimaryKey val driverId: String,
    val passwordHash: String,
    val fullName: String = "",
    val mobileNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
