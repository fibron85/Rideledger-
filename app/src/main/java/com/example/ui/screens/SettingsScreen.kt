package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.BillingCycle
import com.example.domain.FinanceUiState
import com.example.domain.RideCalculator
import com.example.ui.components.AboutAppSectionCard
import com.example.ui.components.ExportDestinationDialog
import com.example.ui.components.ImportFileBrowserDialog
import com.example.ui.components.ImportPermissionDialog
import com.example.ui.viewmodel.DailyReportGroup
import com.example.ui.viewmodel.PeriodSummary
import com.example.util.LocalStorageManager
import com.example.util.ReportExporter
import com.example.util.RideReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    driver: DriverAccountEntity?,
    appSettings: AppSettingsEntity,
    platforms: List<PlatformEntity>,
    pickupPoints: List<PickupPointEntity>,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    remindersEnabled: Boolean,
    onSetRemindersEnabled: (Boolean) -> Unit,
    financeState: FinanceUiState,
    onUpdateProfile: (fullName: String, mobileNumber: String) -> Unit,
    onChangePassword: (currentPin: String, newPin: String, confirmPin: String) -> Unit,
    onUpdateFuelAndCurrency: (fuelText: String, currencyCode: String) -> Unit,
    onUpdateShiftTiming: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit,
    onAddPlatform: (String) -> Unit,
    onDeletePlatform: (PlatformEntity) -> Unit,
    onAddPickupPoint: (name: String, boltFeeText: String, dtcFeeText: String) -> Unit,
    onUpdatePickupPoint: (PickupPointEntity, newName: String, boltFeeText: String, dtcFeeText: String) -> Unit,
    onDeletePickupPoint: (PickupPointEntity) -> Unit,
    settingsCycle: BillingCycle,
    settingsCycleSummary: PeriodSummary,
    settingsCycleGroups: List<DailyReportGroup>,
    onShiftSettingsCycle: (Int) -> Unit,
    onDeleteCycleData: (BillingCycle) -> Unit,
    grantedFolderUri: String?,
    onFolderPermissionGranted: (String) -> Unit,
    onImportExcel: (Result<List<RideEntity>>) -> Unit,
    onExportCompleted: (Result<Unit>, String) -> Unit,
    onUiMessage: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fullNameInput by remember(driver?.fullName) { mutableStateOf(driver?.fullName ?: "") }
    var mobileInput by remember(driver?.mobileNumber) { mutableStateOf(driver?.mobileNumber ?: "") }

    var fuelRateInput by remember(appSettings.fuelCostPerKm) {
        mutableStateOf(appSettings.fuelCostPerKm.toString())
    }
    var currencyInput by remember(appSettings.currencyCode) {
        mutableStateOf(appSettings.currencyCode)
    }

    var shiftStartHour by remember(appSettings.shiftStartHour) { mutableIntStateOf(appSettings.shiftStartHour) }
    var shiftStartMinute by remember(appSettings.shiftStartMinute) { mutableIntStateOf(appSettings.shiftStartMinute) }
    var shiftEndHour by remember(appSettings.shiftEndHour) { mutableIntStateOf(appSettings.shiftEndHour) }
    var shiftEndMinute by remember(appSettings.shiftEndMinute) { mutableIntStateOf(appSettings.shiftEndMinute) }

    LaunchedEffect(driver?.fullName, driver?.mobileNumber) {
        if (driver != null) {
            fullNameInput = driver.fullName
            mobileInput = driver.mobileNumber
        }
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAddPlatformDialog by remember { mutableStateOf(false) }
    var showAddPickupDialog by remember { mutableStateOf(false) }
    var editingPickup by remember { mutableStateOf<PickupPointEntity?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteCycleWarning by remember { mutableStateOf(false) }
    var pendingBackupFileName by remember { mutableStateOf<String?>(null) }
    var showImportPermissionDialog by remember { mutableStateOf(false) }
    var showImportBrowserDialog by remember { mutableStateOf(false) }
    var browserStartInAppFolder by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotificationPermission by remember {
        mutableStateOf(RideReminderScheduler.hasNotificationPermission(context))
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = RideReminderScheduler.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val updatedPermission = granted || RideReminderScheduler.hasNotificationPermission(context)
        hasNotificationPermission = updatedPermission
        if (updatedPermission) {
            permissionDeniedOnce = false
            onSetRemindersEnabled(true)
            RideReminderScheduler.scheduleDailyReminders(context)
        } else {
            permissionDeniedOnce = true
            onUiMessage("Enable notifications in system settings to receive 4:00 PM & 4:00 AM alerts.")
        }
    }

    fun requestNotificationPermissionSafely() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = RideReminderScheduler.hasNotificationPermission(context)
            if (!hasNotificationPermission && !RideReminderScheduler.openAppNotificationSettings(context)) {
                onUiMessage("Notifications are disabled in Android system settings.")
            }
            return
        }

        if (permissionDeniedOnce) {
            if (!RideReminderScheduler.openAppNotificationSettings(context)) {
                onUiMessage("Please enable Notifications for RideLedger in Android App Settings.")
            }
            return
        }

        runCatching {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }.onFailure {
            if (!RideReminderScheduler.openAppNotificationSettings(context)) {
                onUiMessage("Unable to open system permission prompt; please enable notifications in Android Settings.")
            }
        }
    }

    fun buildBackupBytes(): ByteArray {
        return ReportExporter.buildXlsxBytes(
            driver = driver,
            settings = appSettings,
            periodLabel = settingsCycle.label,
            summary = settingsCycleSummary,
            dailyGroups = settingsCycleGroups
        )
    }

    fun exportBackupToGrantedFolder(folderUri: String, fileName: String) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = buildBackupBytes()
                    LocalStorageManager.writeBytesToTreeFolder(
                        context = context,
                        folderUriString = folderUri,
                        fileName = fileName,
                        mimeType = LocalStorageManager.MIME_XLSX,
                        bytes = bytes
                    ).getOrThrow()
                }
            }
            res.fold(
                onSuccess = { savedName ->
                    val folderName = LocalStorageManager.getTreeFolderDisplayName(context, folderUri)
                    onUiMessage("Backup $savedName saved in \"$folderName\".")
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Failed to save backup in destination folder.")
                }
            )
        }
    }

    fun exportBackupToAppDataFolder(fileName: String) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    LocalStorageManager.saveBytesToAppDataFolder(
                        context = context,
                        fileName = fileName,
                        bytes = buildBackupBytes()
                    ).getOrThrow()
                }
            }
            res.fold(
                onSuccess = { file ->
                    onUiMessage("Backup ${file.name} saved inside App 'data' folder.")
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Failed to save backup in App 'data' folder.")
                }
            )
        }
    }

    val grantFolderAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            LocalStorageManager.persistTreePermission(context, treeUri).fold(
                onSuccess = { uriString ->
                    onFolderPermissionGranted(uriString)
                    val folderName = LocalStorageManager.getTreeFolderDisplayName(context, uriString)
                    onUiMessage("Folder read & write access granted: $folderName")
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Could not save folder permission.")
                }
            )
        }
    }

    val exportFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val targetFileName = pendingBackupFileName
        pendingBackupFileName = null
        if (treeUri != null) {
            LocalStorageManager.persistTreePermission(context, treeUri).fold(
                onSuccess = { uriString ->
                    onFolderPermissionGranted(uriString)
                    if (!targetFileName.isNullOrBlank()) {
                        exportBackupToGrantedFolder(uriString, targetFileName)
                    }
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Could not obtain destination folder access.")
                }
            )
        }
    }

    val systemBackupSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        pendingBackupFileName = null
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    ReportExporter.exportToExcelXlsx(
                        context = context,
                        uri = uri,
                        driver = driver,
                        settings = appSettings,
                        periodLabel = settingsCycle.label,
                        summary = settingsCycleSummary,
                        dailyGroups = settingsCycleGroups
                    )
                }
                onExportCompleted(res, "Excel Backup")
            }
        }
    }

    val importFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            LocalStorageManager.persistTreePermission(context, treeUri).fold(
                onSuccess = { uriString ->
                    onFolderPermissionGranted(uriString)
                    browserStartInAppFolder = false
                    showImportBrowserDialog = true
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Could not access selected folder.")
                }
            )
        }
    }

    val systemImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    ReportExporter.importFromExcelXlsx(
                        context = context,
                        uri = uri,
                        driverId = driver?.driverId ?: "",
                        settings = appSettings
                    )
                }
                onImportExcel(res)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Settings & Configuration",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Manage driver profile, shift timing, fuel rate, platforms, and pickup fees",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Driver Profile & Security
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Driver Profile", style = MaterialTheme.typography.titleMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "ID: ${driver?.driverId ?: "-------"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_full_name_input")
                    )

                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { mobileInput = it },
                        label = { Text("Mobile No. (for local password recovery)") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_mobile_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onUpdateProfile(fullNameInput, mobileInput) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_save_profile_button")
                        ) {
                            Text("Save Profile")
                        }
                        FilledTonalButton(
                            onClick = { showChangePasswordDialog = true },
                            modifier = Modifier.testTag("settings_change_password_button")
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Password")
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Fingerprint Login",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBiometricAvailable) {
                                            "Sign in locally with your device fingerprint"
                                        } else {
                                            "No biometric sensor or fingerprint enrolled on device"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = onToggleBiometric,
                                enabled = isBiometricAvailable,
                                modifier = Modifier.testTag("settings_biometric_switch")
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        // 2. Shift Timing (From & To)
        item {
            val isOvernight = (shiftStartHour * 60 + shiftStartMinute) > (shiftEndHour * 60 + shiftEndMinute)
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Shift Timing (From & To)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Rides inside this window count as daily shift income; months calculate from day 21 to next month day 20",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartTimePicker = true }
                                .testTag("settings_shift_from_button")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SHIFT FROM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = RideCalculator.toTimeLabel(shiftStartHour, shiftStartMinute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndTimePicker = true }
                                .testTag("settings_shift_to_button")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SHIFT TO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = RideCalculator.toTimeLabel(shiftEndHour, shiftEndMinute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isOvernight) {
                            "Overnight shift (${RideCalculator.toTimeLabel(shiftStartHour, shiftStartMinute)} → ${RideCalculator.toTimeLabel(shiftEndHour, shiftEndMinute)} next day)"
                        } else {
                            "Same-day shift (${RideCalculator.toTimeLabel(shiftStartHour, shiftStartMinute)} → ${RideCalculator.toTimeLabel(shiftEndHour, shiftEndMinute)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            onUpdateShiftTiming(
                                shiftStartHour,
                                shiftStartMinute,
                                shiftEndHour,
                                shiftEndMinute
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_shift_button")
                    ) {
                        Text("Save Shift Timing")
                    }
                }
            }
        }

        // 2b. Daily Ride Reminders (4:00 PM & 4:00 AM)
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Daily Ride & Finance Reminders", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Periodic alerts every day at 4:00 PM and 4:00 AM for ride entry & pending Finance due payments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    onSetRemindersEnabled(true)
                                    RideReminderScheduler.scheduleDailyReminders(context)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !RideReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        requestNotificationPermissionSafely()
                                    }
                                } else {
                                    onSetRemindersEnabled(false)
                                    RideReminderScheduler.cancelDailyReminders(context)
                                }
                            },
                            modifier = Modifier.testTag("settings_reminders_switch")
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (remindersEnabled) {
                                    "Scheduled: Everyday at 4:00 PM (16:00) & 4:00 AM (04:00) • Ride entry + Finance dues"
                                } else {
                                    "Reminders paused — toggle ON to get 4:00 PM & 4:00 AM alerts"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (remindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || !hasNotificationPermission) {
                                Text(
                                    text = if (hasNotificationPermission) {
                                        "System notification permission: Allowed"
                                    } else {
                                        "System notification permission: Not allowed yet (in-app reminders still active)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasNotificationPermission) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (!hasNotificationPermission) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { requestNotificationPermissionSafely() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_allow_notifications_button")
                            ) {
                                Text("Allow Notification", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (!RideReminderScheduler.openAppNotificationSettings(context)) {
                                        onUiMessage("Open Android Settings → Apps → RideLedger → Notifications to allow alerts.")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_system_notifications_button")
                            ) {
                                Text("App Settings", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // 3. Monthly Backup, Import & Delete (21st-20th Billing Cycle)
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .testTag("settings_monthly_data_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Monthly Backup, Import & Delete", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Back up or delete rides by monthly income cycle (day 21 to day 20), or restore from an Excel backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onShiftSettingsCycle(-1) },
                                    modifier = Modifier.testTag("settings_prev_cycle")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous monthly cycle"
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = settingsCycle.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "21st–20th Billing Month",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onShiftSettingsCycle(1) },
                                    modifier = Modifier.testTag("settings_next_cycle")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next monthly cycle"
                                    )
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${settingsCycleSummary.rideCount} rides",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Fare ${RideCalculator.formatMoney(settingsCycleSummary.grossIncome, appSettings.currencyCode)} • Net ${RideCalculator.formatMoney(settingsCycleSummary.netIncome, appSettings.currencyCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    val hasValidFolder = remember(grantedFolderUri) {
                        LocalStorageManager.hasValidTreePermission(context, grantedFolderUri)
                    }
                    val grantedFolderName = remember(grantedFolderUri, hasValidFolder) {
                        if (hasValidFolder) LocalStorageManager.getTreeFolderDisplayName(context, grantedFolderUri)
                        else "Not granted"
                    }
                    val appDataPath = remember { LocalStorageManager.getAppDataFolderDisplayPath(context) }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "App data folder: $appDataPath",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Shared folder access: $grantedFolderName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasValidFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    runCatching { grantFolderAccessLauncher.launch(null) }
                                        .onFailure {
                                            onUiMessage("System folder picker unavailable; using App 'data' folder.")
                                        }
                                },
                                modifier = Modifier.testTag("settings_folder_permission_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasValidFolder) "Change" else "Grant")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                pendingBackupFileName = "RideLedger_Backup_${settingsCycle.startDateIso}_to_${settingsCycle.endDateIso}.xlsx"
                            },
                            enabled = settingsCycleGroups.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_backup_month_button")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Excel")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (LocalStorageManager.hasValidTreePermission(context, grantedFolderUri)) {
                                    browserStartInAppFolder = false
                                    showImportBrowserDialog = true
                                } else {
                                    showImportPermissionDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_import_excel_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Excel")
                        }
                    }

                    OutlinedButton(
                        onClick = { showDeleteCycleWarning = true },
                        enabled = settingsCycleSummary.rideCount > 0,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_delete_month_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Month Data (${settingsCycle.label})")
                    }
                }
            }
        }

        // 4. Universal Fuel Cost per km Hired Distance & Currency
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Universal Fuel Cost", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Applied automatically per km of distance on all standard rides",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fuelRateInput,
                        onValueChange = { input ->
                            fuelRateInput = input.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Fuel Cost Per km ($currencyInput / km)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_fuel_rate_input")
                    )

                    Text("Currency", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("AED", "USD", "EUR", "GBP", "SAR", "INR").forEach { code ->
                            FilterChip(
                                selected = currencyInput.equals(code, ignoreCase = true),
                                onClick = { currencyInput = code },
                                label = { Text(code) }
                            )
                        }
                    }

                    Button(
                        onClick = { onUpdateFuelAndCurrency(fuelRateInput, currencyInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_fuel_button")
                    ) {
                        Text("Save Fuel & Currency")
                    }
                }
            }
        }

        // 4. Ride-Sharing Platforms
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Ride-Sharing Platforms", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Bolt, DTC, Uber & DTC Event Booking are locked default platforms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { showAddPlatformDialog = true },
                            modifier = Modifier.testTag("settings_add_platform_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    val orderedPlatforms = remember(platforms) {
                        platforms.sortedWith(
                            compareBy<PlatformEntity>(
                                { !RideCalculator.isDefaultPlatform(it.name) },
                                {
                                    val idx = RideCalculator.DEFAULT_PLATFORM_NAMES.indexOfFirst { def ->
                                        def.equals(RideCalculator.normalizePlatformInput(it.name), ignoreCase = true)
                                    }
                                    if (idx >= 0) idx else Int.MAX_VALUE
                                },
                                { it.name.lowercase() }
                            )
                        )
                    }

                    if (orderedPlatforms.isEmpty()) {
                        Text(
                            text = "No platforms added yet. Tap Add to create a custom platform.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        orderedPlatforms.forEachIndexed { index, platform ->
                            if (index > 0) HorizontalDivider()
                            val isDefault = RideCalculator.isDefaultPlatform(platform.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = platform.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isDefault) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = "Default",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    val hint = when {
                                        RideCalculator.isBoltPlatform(platform.name) -> "Uses Pickup Point, Booking Fee & Street Hail/OTP Commission"
                                        RideCalculator.isDtcPlatform(platform.name) -> "Uses Pickup Point & Booking Fee"
                                        RideCalculator.isDtcEventBookingPlatform(platform.name) -> "Event platform (Account payment only; Full Fare = Net Income)"
                                        RideCalculator.isUberPlatform(platform.name) -> "Direct platform (Uses 'Your Earning'; Salik & Pickup hidden)"
                                        RideCalculator.isCareemPlatform(platform.name) -> "Direct platform (Uses 'Your Earning'; Pickup Point hidden)"
                                        else -> "Custom platform (Pickup Point & Booking Fee hidden)"
                                    }
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isDefault) {
                                    androidx.compose.foundation.layout.Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("default_platform_lock_${platform.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "${platform.name} is a default platform and cannot be deleted",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { onDeletePlatform(platform) },
                                        modifier = Modifier.testTag("delete_platform_${platform.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${platform.name}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Pickup Points & Default Bolt/DTC Booking Fees
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Pickup Points & Fees", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Configure default Bolt & DTC booking fees",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showAddPickupDialog = true },
                            modifier = Modifier.testTag("settings_add_pickup_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    if (pickupPoints.isEmpty()) {
                        Text(
                            text = "No pickup points yet. Add points like Street Hail, Bolt OTP, or Airport.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        pickupPoints.forEachIndexed { index, pickup ->
                            if (index > 0) HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pickup.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Bolt Fee: ${RideCalculator.formatMoney(pickup.boltBookingFee, appSettings.currencyCode)} • DTC Fee: ${RideCalculator.formatMoney(pickup.dtcBookingFee, appSettings.currencyCode)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editingPickup = pickup }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit ${pickup.name}"
                                        )
                                    }
                                    IconButton(onClick = { onDeletePickupPoint(pickup) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${pickup.name}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. About
        item {
            AboutAppSectionCard(
                modifier = Modifier.widthIn(max = 720.dp),
                testTag = "settings_about_section"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showStartTimePicker) {
        val state = rememberTimePickerState(
            initialHour = shiftStartHour,
            initialMinute = shiftStartMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Shift From Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shiftStartHour = state.hour
                        shiftStartMinute = state.minute
                        showStartTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(
            initialHour = shiftEndHour,
            initialMinute = shiftEndMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Shift To Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shiftEndHour = state.hour
                        shiftEndMinute = state.minute
                        showEndTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddPlatformDialog) {
        AddPlatformDialog(
            existingPlatformNames = platforms.map { it.name },
            onDismiss = { showAddPlatformDialog = false },
            onConfirm = { name ->
                onAddPlatform(name)
                showAddPlatformDialog = false
            }
        )
    }

    if (showAddPickupDialog) {
        PickupPointDialog(
            title = "Add Pickup Point",
            initialName = "",
            initialBoltFee = "",
            initialDtcFee = "",
            currencyCode = appSettings.currencyCode,
            onDismiss = { showAddPickupDialog = false },
            onConfirm = { name, boltFee, dtcFee ->
                onAddPickupPoint(name, boltFee, dtcFee)
                showAddPickupDialog = false
            }
        )
    }

    editingPickup?.let { target ->
        PickupPointDialog(
            title = "Edit Pickup Point",
            initialName = target.name,
            initialBoltFee = target.boltBookingFee.toString(),
            initialDtcFee = target.dtcBookingFee.toString(),
            currencyCode = appSettings.currencyCode,
            onDismiss = { editingPickup = null },
            onConfirm = { name, boltFee, dtcFee ->
                onUpdatePickupPoint(target, name, boltFee, dtcFee)
                editingPickup = null
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPin, newPin, confirmPin ->
                onChangePassword(currentPin, newPin, confirmPin)
                showChangePasswordDialog = false
            }
        )
    }

    if (showDeleteCycleWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteCycleWarning = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Month Data?") },
            text = {
                Text(
                    "Warning: You are about to permanently delete all ${settingsCycleSummary.rideCount} rides " +
                        "(Gross ${RideCalculator.formatMoney(settingsCycleSummary.grossIncome, appSettings.currencyCode)}, " +
                        "Net ${RideCalculator.formatMoney(settingsCycleSummary.netIncome, appSettings.currencyCode)}) " +
                        "for ${settingsCycle.label}.\n\nThis cannot be undone. Please export an Excel backup before deleting if you want to keep these records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCycleData(settingsCycle)
                        showDeleteCycleWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("settings_confirm_delete_month_button")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCycleWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingBackupFileName?.let { fileName ->
        ExportDestinationDialog(
            fileName = fileName,
            formatLabel = "Excel Backup",
            grantedFolderUri = grantedFolderUri,
            onDismiss = { pendingBackupFileName = null },
            onSaveToGrantedFolder = {
                val uri = grantedFolderUri
                pendingBackupFileName = null
                if (!uri.isNullOrBlank()) {
                    exportBackupToGrantedFolder(uri, fileName)
                }
            },
            onChooseDestinationFolder = {
                runCatching { exportFolderPickerLauncher.launch(null) }
                    .onFailure {
                        pendingBackupFileName = null
                        onUiMessage("Folder picker unavailable; saving backup in App 'data' folder.")
                        exportBackupToAppDataFolder(fileName)
                    }
            },
            onSaveToAppDataFolder = {
                pendingBackupFileName = null
                exportBackupToAppDataFolder(fileName)
            },
            onUseSystemSavePicker = {
                runCatching { systemBackupSaveLauncher.launch(fileName) }
                    .onFailure {
                        pendingBackupFileName = null
                        onUiMessage("System file picker unavailable; saving backup in App 'data' folder.")
                        exportBackupToAppDataFolder(fileName)
                    }
            }
        )
    }

    if (showImportPermissionDialog) {
        ImportPermissionDialog(
            onDismiss = { showImportPermissionDialog = false },
            onGrantFolderPermission = {
                showImportPermissionDialog = false
                runCatching { importFolderPickerLauncher.launch(null) }
                    .onFailure {
                        onUiMessage("System folder picker unavailable; opening App 'data' folder browser.")
                        browserStartInAppFolder = true
                        showImportBrowserDialog = true
                    }
            },
            onBrowseAppDataFolder = {
                showImportPermissionDialog = false
                browserStartInAppFolder = true
                showImportBrowserDialog = true
            },
            onOpenSystemPicker = {
                showImportPermissionDialog = false
                runCatching { systemImportLauncher.launch(arrayOf("*/*")) }
                    .onFailure {
                        onUiMessage("System file picker unavailable; opening App 'data' folder browser.")
                        browserStartInAppFolder = true
                        showImportBrowserDialog = true
                    }
            }
        )
    }

    if (showImportBrowserDialog) {
        ImportFileBrowserDialog(
            grantedFolderUri = grantedFolderUri,
            initialUseAppFolder = browserStartInAppFolder,
            onDismiss = { showImportBrowserDialog = false },
            onSelectEntry = { entry ->
                showImportBrowserDialog = false
                scope.launch {
                    val res = withContext(Dispatchers.IO) {
                        val driverId = driver?.driverId ?: ""
                        when {
                            entry.localFile != null -> ReportExporter.importFromExcelFile(
                                file = entry.localFile,
                                driverId = driverId,
                                settings = appSettings
                            )
                            entry.documentUri != null -> ReportExporter.importFromExcelXlsx(
                                context = context,
                                uri = entry.documentUri,
                                driverId = driverId,
                                settings = appSettings
                            )
                            else -> Result.failure(IllegalStateException("Selected file is unreadable."))
                        }
                    }
                    onImportExcel(res)
                }
            },
            onChangeFolderPermission = {
                showImportBrowserDialog = false
                runCatching { importFolderPickerLauncher.launch(null) }
                    .onFailure {
                        onUiMessage("System folder picker unavailable on this device.")
                    }
            },
            onOpenSystemPicker = {
                showImportBrowserDialog = false
                runCatching { systemImportLauncher.launch(arrayOf("*/*")) }
                    .onFailure {
                        onUiMessage("System file picker unavailable on this device.")
                    }
            }
        )
    }
}

@Composable
private fun AddPlatformDialog(
    existingPlatformNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val cleanInput = remember(name) { RideCalculator.normalizePlatformInput(name) }
    val matchingExisting = remember(cleanInput, existingPlatformNames) {
        if (cleanInput.isEmpty()) null
        else existingPlatformNames.firstOrNull {
            RideCalculator.normalizePlatformInput(it).equals(cleanInput, ignoreCase = true)
        }
    }
    val isDuplicate = matchingExisting != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ride-Sharing Platform") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Platform Name") },
                placeholder = { Text("e.g. Careem, Yango, InDrive") },
                singleLine = true,
                isError = isDuplicate,
                supportingText = {
                    if (isDuplicate) {
                        Text("\"$matchingExisting\" already exists. Please use a different name.")
                    } else {
                        Text("Default platforms (Bolt, DTC, Uber, DTC Event Booking) are already included.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_platform_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cleanInput) },
                enabled = cleanInput.isNotEmpty() && !isDuplicate,
                modifier = Modifier.testTag("dialog_platform_save_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PickupPointDialog(
    title: String,
    initialName: String,
    initialBoltFee: String,
    initialDtcFee: String,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, boltFee: String, dtcFee: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var boltFee by remember { mutableStateOf(initialBoltFee) }
    var dtcFee by remember { mutableStateOf(initialDtcFee) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Pickup Point Name") },
                    placeholder = { Text("e.g. Street Hail, Bolt OTP, Airport") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_pickup_name_input")
                )
                OutlinedTextField(
                    value = boltFee,
                    onValueChange = { boltFee = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Bolt Booking Fee ($currencyCode)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_pickup_bolt_fee_input")
                )
                OutlinedTextField(
                    value = dtcFee,
                    onValueChange = { dtcFee = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("DTC Booking Fee ($currencyCode)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_pickup_dtc_fee_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, boltFee, dtcFee) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("dialog_pickup_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPin: String, newPin: String, confirmPin: String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change 6-Digit Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Current 6-Digit Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("New 6-Digit Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentPin, newPin, confirmPin) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
