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
import com.example.domain.RideCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Database(
    entities = [
        DriverAccountEntity::class,
        PlatformEntity::class,
        PickupPointEntity::class,
        AppSettingsEntity::class,
        RideEntity::class
    ],
    version = 7,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN tips REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Canonicalize default platform casing in existing rides
                db.execSQL("UPDATE rides SET platformName = 'Bolt' WHERE LOWER(TRIM(platformName)) = 'bolt'")
                db.execSQL("UPDATE rides SET platformName = 'DTC' WHERE LOWER(TRIM(platformName)) = 'dtc'")
                db.execSQL("UPDATE rides SET platformName = 'Uber' WHERE LOWER(TRIM(platformName)) = 'uber'")
                db.execSQL("UPDATE rides SET platformName = 'DTC Event Booking' WHERE LOWER(TRIM(platformName)) = 'dtc event booking'")

                // Deduplicate case-insensitive platform rows (e.g., keeping one 'Bolt' and deleting 'BOLT')
                val keepByKey = linkedMapOf<String, Pair<Long, String>>()
                val allIds = mutableListOf<Long>()
                db.query("SELECT id, name FROM platforms ORDER BY id ASC").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val name = cursor.getString(1) ?: ""
                        allIds.add(id)
                        val normalized = name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.US)
                        val existing = keepByKey[normalized]
                        if (existing == null) {
                            keepByKey[normalized] = id to name
                        } else {
                            val canonical = canonicalDefaultName(normalized)
                            if (canonical != null && name.trim() == canonical && existing.second.trim() != canonical) {
                                keepByKey[normalized] = id to name
                            }
                        }
                    }
                }
                val keepIds = keepByKey.values.map { it.first }.toSet()
                allIds.filter { it !in keepIds }.forEach { dupId ->
                    db.execSQL("DELETE FROM platforms WHERE id = ?", arrayOf(dupId))
                }
                keepByKey.forEach { (normalized, pair) ->
                    val canonical = canonicalDefaultName(normalized)
                    if (canonical != null && pair.second != canonical) {
                        db.execSQL("UPDATE platforms SET name = ? WHERE id = ?", arrayOf(canonical, pair.first))
                    }
                }
            }
        }

        private fun canonicalDefaultName(normalizedLower: String): String? = when (normalizedLower) {
            "bolt" -> "Bolt"
            "dtc" -> "DTC"
            "uber" -> "Uber"
            "dtc event booking" -> "DTC Event Booking"
            else -> null
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ride_ledger_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
            // Deduplicate any pre-existing case/space duplicate platforms (e.g. Bolt + BOLT)
            val existingPlatforms = settingsDao.getAllPlatformsOnce()
            val groupedByKey = existingPlatforms.groupBy {
                RideCalculator.normalizePlatformInput(it.name).lowercase(Locale.US)
            }
            val presentKeys = mutableSetOf<String>()
            groupedByKey.forEach { (key, list) ->
                if (key.isBlank()) {
                    list.forEach { settingsDao.deletePlatform(it) }
                } else {
                    val canonical = canonicalDefaultName(key)
                    val keeper = (if (canonical != null) {
                        list.firstOrNull { it.name == canonical }
                    } else null) ?: list.minBy { it.id }

                    list.filter { it.id != keeper.id }.forEach { duplicate ->
                        settingsDao.deletePlatform(duplicate)
                    }
                    if (canonical != null && keeper.name != canonical) {
                        settingsDao.updatePlatform(keeper.copy(name = canonical))
                    }
                    presentKeys.add(key)
                }
            }
            RideCalculator.DEFAULT_PLATFORM_NAMES.forEach { defaultName ->
                val normalized = defaultName.lowercase(Locale.US)
                if (normalized !in presentKeys) {
                    settingsDao.insertPlatform(PlatformEntity(name = defaultName))
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
