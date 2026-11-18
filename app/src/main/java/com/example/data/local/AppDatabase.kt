package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun driverDao(): DriverDao
    abstract fun settingsDao(): SettingsDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN shiftStartHour INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN shiftStartMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN shiftEndHour INTEGER NOT NULL DEFAULT 23")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN shiftEndMinute INTEGER NOT NULL DEFAULT 59")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Cash'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN isSubmittedToFinance INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rides ADD COLUMN financeSubmittedAt INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT OR IGNORE INTO platforms (name, createdAt) VALUES ('DTC Event Booking', $now)"
                )
                db.execSQL(
                    "DELETE FROM pickup_points WHERE LOWER(TRIM(name)) = 'dtc event booking'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ride_ledger_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
                        currencyCode = "AED",
                        shiftStartHour = 0,
                        shiftStartMinute = 0,
                        shiftEndHour = 23,
                        shiftEndMinute = 59
                    )
                )
            }
            if (settingsDao.getPlatformCount() == 0) {
                listOf("Bolt", "DTC", "DTC Event Booking", "Uber", "Careem").forEach { name ->
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
