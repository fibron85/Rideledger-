package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.repository.RideLedgerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: RideLedgerRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RideLedgerRepository(
            driverDao = database.driverDao(),
            settingsDao = database.settingsDao(),
            rideDao = database.rideDao(),
            sessionManager = SessionManager(context)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("RideLedger", appName)
    }

    @Test
    fun `local driver registration profile setup and local password reset succeed`() = runTest {
        val registerResult = repository.registerDriver("1234567", "112233")
        assertTrue(registerResult.isSuccess)

        val profileResult = repository.updateProfile("1234567", "Tariq Ali", "0509876543")
        assertTrue(profileResult.isSuccess)

        // Wrong mobile should fail password reset
        val wrongReset = repository.resetPassword("1234567", "0500000000", "998877")
        assertTrue(wrongReset.isFailure)

        // Correct mobile resets password locally
        val validReset = repository.resetPassword("1234567", "0509876543", "998877")
        assertTrue(validReset.isSuccess)

        // Old password fails, new password succeeds
        assertTrue(repository.loginDriver("1234567", "112233").isFailure)
        assertTrue(repository.loginDriver("1234567", "998877").isSuccess)
    }

    @Test
    fun `biometric enrollment persists across logout and finance submission clears due balance`() = runTest {
        repository.registerDriver("7654321", "123456")
        repository.updateProfile("7654321", "Amina", "0501112233")
        repository.setBiometricEnabled("7654321", true)

        repository.logout()
        val bioDriverId = repository.biometricDriverId.first()
        assertEquals("7654321", bioDriverId)
        assertTrue(repository.loginWithBiometric("7654321").isSuccess)

        val ts = System.currentTimeMillis()
        val idCash = repository.saveRide(
            com.example.data.local.entity.RideEntity(
                driverId = "7654321",
                rideDateMillis = ts,
                rideDateIso = "2026-11-17",
                platformName = "Bolt",
                pickupPointName = "Street Hail",
                paymentMethod = "Cash",
                distanceKm = 12.0,
                grossFare = 60.0,
                fuelCostPerKmApplied = 0.45,
                fuelCost = 5.4,
                salikCost = 0.0,
                boltCommission = 5.0,
                bookingFee = 0.0,
                netIncome = 49.6
            )
        )
        val idCard = repository.saveRide(
            com.example.data.local.entity.RideEntity(
                driverId = "7654321",
                rideDateMillis = ts,
                rideDateIso = "2026-11-17",
                platformName = "Uber",
                pickupPointName = "",
                paymentMethod = "Card",
                distanceKm = 8.0,
                grossFare = 40.0,
                fuelCostPerKmApplied = 0.45,
                fuelCost = 3.6,
                salikCost = 0.0,
                boltCommission = 0.0,
                bookingFee = 0.0,
                netIncome = 36.4
            )
        )

        repository.submitRidesToFinance(listOf(idCash, idCard))
        val savedCash = repository.getRideById(idCash)!!
        val savedCard = repository.getRideById(idCard)!!
        assertTrue(savedCash.isSubmittedToFinance)
        assertTrue(savedCard.isSubmittedToFinance)
    }

    @Test
    fun `deleteRidesForCycle removes only target month and importBackupRides restores without duplicates`() = runTest {
        val driverId = "2233445"
        repository.registerDriver(driverId, "123456")
        val settings = com.example.data.local.entity.AppSettingsEntity()

        val nov10 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 10, 14, 0, 0) }.timeInMillis
        val nov15 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 15, 16, 30, 0) }.timeInMillis
        val nov25 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 25, 11, 0, 0) }.timeInMillis

        val novRide1 = com.example.data.local.entity.RideEntity(
            driverId = driverId, rideDateMillis = nov10, rideDateIso = "2026-11-10",
            platformName = "Careem", pickupPointName = "", paymentMethod = "Card",
            distanceKm = 12.0, grossFare = 70.0, fuelCostPerKmApplied = 0.45,
            fuelCost = 5.4, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0, netIncome = 64.6
        )
        val novRide2 = com.example.data.local.entity.RideEntity(
            driverId = driverId, rideDateMillis = nov15, rideDateIso = "2026-11-15",
            platformName = "DTC Event Booking", pickupPointName = "", paymentMethod = "Account",
            distanceKm = 0.0, grossFare = 240.0, fuelCostPerKmApplied = 0.0,
            fuelCost = 0.0, salikCost = 0.0, boltCommission = 0.0, bookingFee = 0.0, netIncome = 240.0
        )
        val decCycleRide = com.example.data.local.entity.RideEntity(
            driverId = driverId, rideDateMillis = nov25, rideDateIso = "2026-11-25",
            platformName = "Bolt", pickupPointName = "Street Hail", paymentMethod = "Cash",
            distanceKm = 9.0, grossFare = 50.0, fuelCostPerKmApplied = 0.45,
            fuelCost = 4.05, salikCost = 0.0, boltCommission = 5.0, bookingFee = 0.0, netIncome = 40.95
        )

        repository.saveRide(novRide1)
        repository.saveRide(novRide2)
        repository.saveRide(decCycleRide)

        // Back up November cycle (21 Oct - 20 Nov 2026) before deleting
        val novCycle = com.example.domain.RideCalculator.getBillingCycleForDate("2026-11-10")
        val backupBytes = com.example.util.ReportExporter.buildXlsxBytes(
            driver = com.example.data.local.entity.DriverAccountEntity(driverId, "h", "Driver", "050"),
            settings = settings,
            periodLabel = novCycle.label,
            summary = com.example.ui.viewmodel.PeriodSummary(310.0, 304.6, 5.4, 0.0, 0.0, 0.0, 12.0, 2),
            dailyGroups = listOf(
                com.example.ui.viewmodel.DailyReportGroup("2026-11-10", com.example.ui.viewmodel.PeriodSummary(70.0, 64.6, 5.4, 0.0, 0.0, 0.0, 12.0, 1), listOf(novRide1)),
                com.example.ui.viewmodel.DailyReportGroup("2026-11-15", com.example.ui.viewmodel.PeriodSummary(240.0, 240.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1), listOf(novRide2))
            )
        )

        val deletedCount = repository.deleteRidesForCycle(driverId, novCycle, settings)
        assertEquals(2, deletedCount)

        val remaining = repository.observeAllRides(driverId).first()
        assertEquals(1, remaining.size)
        assertEquals("2026-11-25", remaining.first().rideDateIso)

        // Import from Excel backup restores the 2 deleted rides
        val parsed = com.example.util.ReportExporter.parseXlsxBytes(backupBytes, driverId, settings)
        val firstImport = repository.importBackupRides(driverId, parsed)
        assertEquals(2, firstImport.importedCount)
        assertEquals(0, firstImport.skippedDuplicates)
        assertEquals(3, repository.observeAllRides(driverId).first().size)

        // Second import skips duplicates cleanly
        val secondImport = repository.importBackupRides(driverId, parsed)
        assertEquals(0, secondImport.importedCount)
        assertEquals(2, secondImport.skippedDuplicates)
        assertEquals(3, repository.observeAllRides(driverId).first().size)
    }

    @Test
    fun `app data folder is created, syncs saved rides, lists xlsx files, and imports cleanly`() = runTest {
        val driverId = "9988776"
        repository.registerDriver(driverId, "123456")
        repository.updateProfile(driverId, "Karim", "0501239876")
        val driver = repository.observeDriver(driverId).first()!!
        val settings = com.example.data.local.entity.AppSettingsEntity()

        val ride = com.example.data.local.entity.RideEntity(
            driverId = driverId,
            rideDateMillis = System.currentTimeMillis(),
            rideDateIso = "2026-11-17",
            platformName = "Uber",
            pickupPointName = "",
            paymentMethod = "Cash",
            distanceKm = 10.0,
            grossFare = 85.0,
            fuelCostPerKmApplied = 0.45,
            fuelCost = 4.5,
            salikCost = 0.0,
            boltCommission = 0.0,
            bookingFee = 0.0,
            netIncome = 80.5
        )
        repository.saveRide(ride)

        val dataFolder = com.example.util.LocalStorageManager.ensureAppDataFolder(context)
        assertTrue(dataFolder.exists() && dataFolder.isDirectory)
        assertEquals("data", dataFolder.name)

        val syncRes = com.example.util.LocalStorageManager.syncDriverDataBackup(
            context = context,
            driver = driver,
            settings = settings,
            rides = listOf(ride)
        )
        assertTrue(syncRes.isSuccess)

        val listed = com.example.util.LocalStorageManager.listLocalFolderChildren(dataFolder)
        assertTrue(listed.any { !it.isDirectory && it.name == "RideLedger_${driverId}_data.xlsx" })

        val backupFile = listed.first { it.name == "RideLedger_${driverId}_data.xlsx" }.localFile!!
        val imported = com.example.util.ReportExporter.importFromExcelFile(backupFile, driverId, settings)
        assertTrue(imported.isSuccess)
        assertEquals(1, imported.getOrThrow().size)
        assertEquals("Uber", imported.getOrThrow().first().platformName)

        // SessionManager persists setup & folder URI flags
        assertEquals(false, repository.storageSetupCompleted.first())
        repository.setStorageSetupCompleted(true)
        repository.setGrantedFolderUri("content://com.android.externalstorage.documents/tree/primary%3ADocuments")
        assertEquals(true, repository.storageSetupCompleted.first())
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
            repository.grantedFolderUri.first()
        )
    }
}
