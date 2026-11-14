package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DriverDao
import com.example.data.local.dao.RideDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.data.local.entity.RideEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DriverAccountEntity::class,
        PlatformEntity::class,
        PickupPointEntity::class,
        AppSettingsEntity::class,
        RideEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun driverDao(): DriverDao
    abstract fun settingsDao(): SettingsDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ride_ledger_local.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { seedInitialData(it.settingsDao()) }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun seedInitialData(settingsDao: SettingsDao) {
            if (settingsDao.getAppSettings() == null) {
                settingsDao.upsertAppSettings(
                    AppSettingsEntity(
                        id = 1,
                        fuelCostPerKm = 0.45,
                        currencyCode = "AED"
                    )
                )
            }
            if (settingsDao.getPlatformCount() == 0) {
                listOf("Bolt", "DTC", "Uber", "Careem").forEach { name ->
                    settingsDao.insertPlatform(PlatformEntity(name = name))
                }
            }
            if (settingsDao.getPickupPointCount() == 0) {
                listOf(
                    PickupPointEntity(name = "Street Hail", boltBookingFee = 0.0, dtcBookingFee = 0.0),
                    PickupPointEntity(name = "Bolt OTP", boltBookingFee = 0.0, dtcBookingFee = 0.0),
                    PickupPointEntity(name = "Airport", boltBookingFee = 20.0, dtcBookingFee = 25.0),
                    PickupPointEntity(name = "City Center", boltBookingFee = 5.0, dtcBookingFee = 5.0)
                ).forEach { pickup ->
                    settingsDao.insertPickupPoint(pickup)
                }
            }
        }
    }
}
