package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DriverAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {

    @Query("SELECT * FROM driver_accounts WHERE driverId = :driverId LIMIT 1")
    fun observeDriver(driverId: String): Flow<DriverAccountEntity?>

    @Query("SELECT * FROM driver_accounts WHERE driverId = :driverId LIMIT 1")
    suspend fun getDriverById(driverId: String): DriverAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDriver(driver: DriverAccountEntity)

    @Update
    suspend fun updateDriver(driver: DriverAccountEntity)

    @Query("UPDATE driver_accounts SET fullName = :fullName, mobileNumber = :mobileNumber WHERE driverId = :driverId")
    suspend fun updateProfile(driverId: String, fullName: String, mobileNumber: String)

    @Query("UPDATE driver_accounts SET passwordHash = :passwordHash WHERE driverId = :driverId")
    suspend fun updatePassword(driverId: String, passwordHash: String)
}
