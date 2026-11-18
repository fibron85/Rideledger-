package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val fuelCostPerKm: Double = 0.45,
    val currencyCode: String = "AED",
    val shiftStartHour: Int = 0,
    val shiftStartMinute: Int = 0,
    val shiftEndHour: Int = 23,
    val shiftEndMinute: Int = 59
) {
    val shiftStartTotalMinutes: Int
        get() = (shiftStartHour.coerceIn(0, 23) * 60) + shiftStartMinute.coerceIn(0, 59)

    val shiftEndTotalMinutes: Int
        get() = (shiftEndHour.coerceIn(0, 23) * 60) + shiftEndMinute.coerceIn(0, 59)
}
