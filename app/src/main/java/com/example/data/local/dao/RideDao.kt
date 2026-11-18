package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Query("SELECT * FROM rides WHERE driverId = :driverId ORDER BY rideDateIso DESC, createdAt DESC")
    fun observeAllRidesForDriver(driverId: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE driverId = :driverId AND rideDateIso = :dateIso ORDER BY createdAt DESC")
    fun observeRidesForDate(driverId: String, dateIso: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE driverId = :driverId AND rideDateIso LIKE :monthPrefix || '%' ORDER BY rideDateIso DESC, createdAt DESC")
    fun observeRidesForMonth(driverId: String, monthPrefix: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId LIMIT 1")
    suspend fun getRideById(rideId: Long): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Query("SELECT * FROM rides WHERE driverId = :driverId ORDER BY rideDateIso DESC, createdAt DESC")
    suspend fun getAllRidesForDriverOnce(driverId: String): List<RideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRides(rides: List<RideEntity>)

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Query("DELETE FROM rides WHERE id IN (:rideIds)")
    suspend fun deleteRidesByIds(rideIds: List<Long>)

    @Query("UPDATE rides SET isSubmittedToFinance = 1, financeSubmittedAt = :submittedAt WHERE id IN (:rideIds)")
    suspend fun markRidesSubmittedToFinance(rideIds: List<Long>, submittedAt: Long)
}
