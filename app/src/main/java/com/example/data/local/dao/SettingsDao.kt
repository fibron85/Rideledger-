package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM platforms ORDER BY name COLLATE NOCASE ASC")
    fun observePlatforms(): Flow<List<PlatformEntity>>

    @Query("SELECT * FROM platforms")
    suspend fun getAllPlatformsOnce(): List<PlatformEntity>

    @Query("SELECT COUNT(*) FROM platforms")
    suspend fun getPlatformCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlatform(platform: PlatformEntity): Long

    @Update
    suspend fun updatePlatform(platform: PlatformEntity)

    @Delete
    suspend fun deletePlatform(platform: PlatformEntity)

    @Query("SELECT * FROM pickup_points ORDER BY name COLLATE NOCASE ASC")
    fun observePickupPoints(): Flow<List<PickupPointEntity>>

    @Query("SELECT COUNT(*) FROM pickup_points")
    suspend fun getPickupPointCount(): Int

    @Query("SELECT * FROM pickup_points WHERE name = :name LIMIT 1")
    suspend fun getPickupPointByName(name: String): PickupPointEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPickupPoint(pickupPoint: PickupPointEntity): Long

    @Update
    suspend fun updatePickupPoint(pickupPoint: PickupPointEntity)

    @Delete
    suspend fun deletePickupPoint(pickupPoint: PickupPointEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppSettings(settings: AppSettingsEntity)
}
