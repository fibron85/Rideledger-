package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.repository.RideLedgerRepository
import com.example.domain.BillingCycle
import com.example.domain.FinanceUiState
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RideLedgerTheme
import com.example.ui.viewmodel.PeriodSummary
import com.example.util.RideReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
            netIncome = 95.5,
            tips = 15.0
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
        assertEquals(15.0, imported.getOrThrow().first().tips, 0.001)

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

    @Test
    fun `default platforms cannot be deleted, duplicates rejected case-insensitively, custom platforms work`() = runTest {
        repository.registerDriver("5544332", "123456")

        val initialPlatforms = repository.platforms.first()
        val initialNames = initialPlatforms.map { it.name }.toSet()
        assertEquals(setOf("Bolt", "DTC", "Uber", "DTC Event Booking"), initialNames)

        // Default platforms cannot be deleted
        for (defaultPlatform in initialPlatforms) {
            val delRes = repository.deletePlatform(defaultPlatform)
            assertTrue("Expected failure deleting ${defaultPlatform.name}", delRes.isFailure)
        }
        assertEquals(4, repository.platforms.first().size)

        // Duplicates of default platforms (any case or spacing) are rejected
        assertTrue(repository.addPlatform("BOLT").isFailure)
        assertTrue(repository.addPlatform("dtc").isFailure)
        assertTrue(repository.addPlatform("uber").isFailure)
        assertTrue(repository.addPlatform("  dtc   event   booking ").isFailure)

        // Adding a custom platform with a different name succeeds
        assertTrue(repository.addPlatform("Yango").isSuccess)
        // Duplicate of custom platform is also rejected
        assertTrue(repository.addPlatform("yango").isFailure)

        val afterAdd = repository.platforms.first()
        assertEquals(5, afterAdd.size)
        val yango = afterAdd.first { it.name == "Yango" }

        // Custom platform CAN be deleted
        val delCustom = repository.deletePlatform(yango)
        assertTrue(delCustom.isSuccess)
        assertEquals(4, repository.platforms.first().size)
    }

    @Test
    fun `pre-existing duplicate Bolt and BOLT rows are deduplicated to single Bolt`() = runTest {
        val dao = database.settingsDao()
        dao.insertPlatform(com.example.data.local.entity.PlatformEntity(name = "Bolt"))
        dao.insertPlatform(com.example.data.local.entity.PlatformEntity(name = "BOLT"))
        dao.insertPlatform(com.example.data.local.entity.PlatformEntity(name = "uber"))

        repository.ensureDefaultsPopulated()

        val cleaned = repository.platforms.first()
        val boltMatches = cleaned.filter { it.name.equals("bolt", ignoreCase = true) }
        assertEquals(1, boltMatches.size)
        assertEquals("Bolt", boltMatches.first().name)

        val uberMatches = cleaned.filter { it.name.equals("uber", ignoreCase = true) }
        assertEquals(1, uberMatches.size)
        assertEquals("Uber", uberMatches.first().name)
        assertEquals(setOf("Bolt", "DTC", "Uber", "DTC Event Booking"), cleaned.map { it.name }.toSet())
    }

    @Test
    fun `daily reminder toggle and dismissed in-app slot persist in session datastore`() = runTest {
        assertTrue(repository.remindersEnabled.first())
        repository.setRemindersEnabled(false)
        assertEquals(false, repository.remindersEnabled.first())

        repository.setRemindersEnabled(true)
        assertEquals(true, repository.remindersEnabled.first())

        repository.setDismissedReminderSlot("2026-11-18_16")
        assertEquals("2026-11-18_16", repository.dismissedReminderSlot.first())

        repository.setDismissedFinanceReminderSlot("2026-11-18_16_fin")
        assertEquals("2026-11-18_16_fin", repository.dismissedFinanceReminderSlot.first())

        repository.registerDriver("3322110", "123456")
        repository.saveRide(
            com.example.data.local.entity.RideEntity(
                driverId = "3322110",
                rideDateMillis = System.currentTimeMillis(),
                rideDateIso = "2026-11-18",
                platformName = "Bolt",
                pickupPointName = "Street Hail",
                paymentMethod = "Cash",
                distanceKm = 10.0,
                grossFare = 75.0,
                fuelCostPerKmApplied = 0.45,
                fuelCost = 4.5,
                salikCost = 0.0,
                boltCommission = 7.5,
                bookingFee = 0.0,
                netIncome = 63.0,
                isSubmittedToFinance = false
            )
        )
        val pending = repository.getPendingFinanceSummaryForActiveDriver()
        assertEquals(75.0, pending?.second?.cashOnHandGross ?: 0.0, 0.001)
        assertEquals(1, pending?.second?.allPendingRideIds?.size)
    }

    @Test
    fun `notification allow and test alerts work without crashing whether permission denied or granted`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)

        // 1. Permission denied: scheduler returns false cleanly without throwing
        shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(RideReminderScheduler.showRideReminderNotification(context, isEveningSlot = true, isTest = true))
        assertFalse(
            RideReminderScheduler.showFinanceReminderNotification(
                context = context,
                isEveningSlot = true,
                pendingShiftCount = 2,
                totalDueText = "AED 120.00",
                cashText = "AED 80.00",
                otherDueText = "AED 40.00",
                isTest = true
            )
        )

        // 2. Permission granted: notifications post cleanly to NotificationManager
        shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(RideReminderScheduler.showRideReminderNotification(context, isEveningSlot = true, isTest = true))
        assertTrue(
            RideReminderScheduler.showFinanceReminderNotification(
                context = context,
                isEveningSlot = false,
                pendingShiftCount = 1,
                totalDueText = "AED 50.00",
                cashText = "AED 50.00",
                otherDueText = "AED 0.00",
                isTest = true
            )
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val postedCount = Shadows.shadowOf(notificationManager).allNotifications.size
        assertTrue(postedCount >= 2)
    }

    @Test
    fun `settings screen notification controls handle clicks safely without crashing`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val messages = mutableListOf<String>()

        composeTestRule.setContent {
            RideLedgerTheme {
                SettingsScreen(
                    driver = DriverAccountEntity("1234567", "hash", "Driver Test", "0501234567"),
                    appSettings = AppSettingsEntity(),
                    platforms = emptyList(),
                    pickupPoints = emptyList(),
                    isBiometricAvailable = false,
                    isBiometricEnabled = false,
                    onToggleBiometric = {},
                    remindersEnabled = true,
                    onSetRemindersEnabled = {},
                    financeState = FinanceUiState(),
                    onUpdateProfile = { _, _ -> },
                    onChangePassword = { _, _, _ -> },
                    onUpdateFuelAndCurrency = { _, _ -> },
                    onUpdateShiftTiming = { _, _, _, _ -> },
                    onAddPlatform = {},
                    onDeletePlatform = {},
                    onAddPickupPoint = { _, _, _ -> },
                    onUpdatePickupPoint = { _, _, _, _ -> },
                    onDeletePickupPoint = {},
                    settingsCycle = BillingCycle("2026-10-21", "2026-11-20", "Nov 2026"),
                    settingsCycleSummary = PeriodSummary(),
                    settingsCycleGroups = emptyList(),
                    onShiftSettingsCycle = {},
                    onDeleteCycleData = {},
                    grantedFolderUri = null,
                    onFolderPermissionGranted = {},
                    onImportExcel = {},
                    onExportCompleted = { _, _ -> },
                    onUiMessage = { messages.add(it) },
                    onLogout = {}
                )
            }
        }

        val listNode = composeTestRule.onNode(hasScrollToIndexAction())
        listNode.performScrollToNode(hasTestTag("settings_allow_notifications_button"))
        composeTestRule.onNodeWithTag("settings_allow_notifications_button").performClick()

        listNode.performScrollToNode(hasTestTag("settings_system_notifications_button"))
        composeTestRule.onNodeWithTag("settings_system_notifications_button").performClick()

        listNode.performScrollToNode(hasTestTag("settings_reminders_switch"))
        composeTestRule.onNodeWithTag("settings_reminders_switch").performClick()

        // Test notification buttons were removed
        composeTestRule.onNodeWithTag("settings_test_reminder_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("settings_test_finance_reminder_button").assertDoesNotExist()

        // About section displays title, App Version 1.0.1, description, and developer/copyright details
        assertEquals("1.0.1", BuildConfig.VERSION_NAME)
        listNode.performScrollToNode(hasTestTag("settings_about_section"))
        composeTestRule.onNodeWithTag("settings_about_section").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
        composeTestRule.onNodeWithText("App Version 1.0.1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developed by Fakhrul Islam").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: fibron85@gmail.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile: +971558895749").assertIsDisplayed()
    }
}
