package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.PlatformEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.BillingCycle
import com.example.domain.RideCalculator
import com.example.ui.components.ExportDestinationDialog
import com.example.ui.components.ImportFileBrowserDialog
import com.example.ui.components.ImportPermissionDialog
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.viewmodel.DailyReportGroup
import com.example.ui.viewmodel.PeriodSummary
import com.example.util.LocalStorageManager
import com.example.util.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PendingReportExport(
    val fileName: String,
    val isPdf: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    driver: DriverAccountEntity?,
    appSettings: AppSettingsEntity,
    platforms: List<PlatformEntity>,
    selectedCycle: BillingCycle,
    selectedDateFilter: String?,
    searchQuery: String,
    selectedPlatformFilter: String?,
    selectedPaymentFilter: String?,
    cycleSummary: PeriodSummary,
    reportGroups: List<DailyReportGroup>,
    onShiftCycle: (Int) -> Unit,
    onDateFilterSelected: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPlatformFilterSelected: (String?) -> Unit,
    onPaymentFilterSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onSubmitShiftToFinance: (rideIds: List<Long>, shiftDateIso: String) -> Unit,
    onEditRide: (RideEntity) -> Unit,
    onDeleteRide: (RideEntity) -> Unit,
    grantedFolderUri: String?,
    onFolderPermissionGranted: (String) -> Unit,
    onExportCompleted: (Result<Unit>, String) -> Unit,
    onImportExcel: (Result<List<RideEntity>>) -> Unit,
    onUiMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currencyCode = appSettings.currencyCode
    val periodLabel = selectedDateFilter?.let { "Shift $it" } ?: selectedCycle.label

    var showCalendarDialog by remember { mutableStateOf(false) }
    var ridePendingDelete by remember { mutableStateOf<RideEntity?>(null) }
    var shiftPendingFinance by remember { mutableStateOf<DailyReportGroup?>(null) }
    var pendingExport by remember { mutableStateOf<PendingReportExport?>(null) }
    var showImportPermissionDialog by remember { mutableStateOf(false) }
    var showImportBrowserDialog by remember { mutableStateOf(false) }
    var browserStartInAppFolder by remember { mutableStateOf(false) }
    var expandedDayIso by remember(reportGroups) {
        mutableStateOf(reportGroups.firstOrNull()?.dateIso)
    }
    val hasActiveFilter = searchQuery.isNotBlank() ||
        selectedPlatformFilter != null ||
        selectedPaymentFilter != null ||
        selectedDateFilter != null

    fun buildExportBytes(isPdf: Boolean): ByteArray {
        return if (isPdf) {
            ReportExporter.buildPdfBytes(
                driver = driver,
                settings = appSettings,
                periodLabel = periodLabel,
                summary = cycleSummary,
                dailyGroups = reportGroups
            )
        } else {
            ReportExporter.buildXlsxBytes(
                driver = driver,
                settings = appSettings,
                periodLabel = periodLabel,
                summary = cycleSummary,
                dailyGroups = reportGroups
            )
        }
    }

    fun exportToGrantedFolder(folderUri: String, request: PendingReportExport) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = buildExportBytes(request.isPdf)
                    val mime = if (request.isPdf) LocalStorageManager.MIME_PDF else LocalStorageManager.MIME_XLSX
                    LocalStorageManager.writeBytesToTreeFolder(
                        context = context,
                        folderUriString = folderUri,
                        fileName = request.fileName,
                        mimeType = mime,
                        bytes = bytes
                    ).getOrThrow()
                }
            }
            res.fold(
                onSuccess = { savedName ->
                    val folderName = LocalStorageManager.getTreeFolderDisplayName(context, folderUri)
                    onUiMessage("Saved $savedName in folder \"$folderName\".")
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Failed to save file to destination folder.")
                }
            )
        }
    }

    fun exportToAppDataFolder(request: PendingReportExport) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = buildExportBytes(request.isPdf)
                    LocalStorageManager.saveBytesToAppDataFolder(
                        context = context,
                        fileName = request.fileName,
                        bytes = bytes
                    ).getOrThrow()
                }
            }
            res.fold(
                onSuccess = { file ->
                    onUiMessage("Saved ${file.name} inside App 'data' folder.")
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Failed to save in App 'data' folder.")
                }
            )
        }
    }

    val exportFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val req = pendingExport
        pendingExport = null
        if (treeUri != null) {
            LocalStorageManager.persistTreePermission(context, treeUri).fold(
                onSuccess = { uriString ->
                    onFolderPermissionGranted(uriString)
                    if (req != null) {
                        exportToGrantedFolder(uriString, req)
                    }
                },
                onFailure = { err ->
                    onUiMessage(err.message ?: "Could not obtain folder write permission.")
                }
            )
        }
    }

    val systemSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val req = pendingExport
        pendingExport = null
        if (uri != null && req != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    if (req.isPdf) {
                        ReportExporter.exportToPdf(
                            context = context,
                            uri = uri,
                            driver = driver,
                            settings = appSettings,
                            periodLabel = periodLabel,
                            summary = cycleSummary,
                            dailyGroups = reportGroups
                        )
                    } else {
                        ReportExporter.exportToExcelXlsx(
                            context = context,
                            uri = uri,
                            driver = driver,
                            settings = appSettings,
                            periodLabel = periodLabel,
                            summary = cycleSummary,
                            dailyGroups = reportGroups
                        )
                    }
                }
                onExportCompleted(res, if (req.isPdf) "PDF" else "Excel")
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 840.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Billing Cycle (21st–20th) / Date Filter Bar + Export Buttons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            IconButton(
                                onClick = { onShiftCycle(-1) },
                                modifier = Modifier.testTag("report_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous billing month")
                            }
                            Column {
                                Text(
                                    text = selectedCycle.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "21st–20th Cycle • Shift ${RideCalculator.formatShiftWindow(appSettings)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onShiftCycle(1) },
                                modifier = Modifier.testTag("report_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next billing month")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selectedDateFilter != null) {
                                FilterChip(
                                    selected = true,
                                    onClick = { onDateFilterSelected(null) },
                                    label = { Text(selectedDateFilter) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Clear day filter", modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            IconButton(
                                onClick = { showCalendarDialog = true },
                                modifier = Modifier.testTag("report_pick_date_button")
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick specific day")
                            }
                        }
                    }

                    // Search & Platform / Payment Method Filters for Daily Shift Income
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search platform, payment method, pickup, note...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shift_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedPlatformFilter == null,
                            onClick = { onPlatformFilterSelected(null) },
                            label = { Text("All Platforms") },
                            modifier = Modifier.testTag("filter_platform_all")
                        )
                        platforms.forEach { platform ->
                            FilterChip(
                                selected = selectedPlatformFilter.equals(platform.name, ignoreCase = true),
                                onClick = {
                                    onPlatformFilterSelected(
                                        if (selectedPlatformFilter.equals(platform.name, ignoreCase = true)) null
                                        else platform.name
                                    )
                                },
                                label = { Text(platform.name) },
                                modifier = Modifier.testTag("filter_platform_${platform.name}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedPaymentFilter == null,
                            onClick = { onPaymentFilterSelected(null) },
                            label = { Text("All Payments") },
                            modifier = Modifier.testTag("filter_payment_all")
                        )
                        RideCalculator.PAYMENT_METHODS.forEach { method ->
                            FilterChip(
                                selected = selectedPaymentFilter.equals(method, ignoreCase = true),
                                onClick = {
                                    onPaymentFilterSelected(
                                        if (selectedPaymentFilter.equals(method, ignoreCase = true)) null
                                        else method
                                    )
                                },
                                label = { Text(method) },
                                modifier = Modifier.testTag("filter_payment_$method")
                            )
                        }
                    }

                    // Period totals summary bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatPill(
                            label = "Rides",
                            value = "${cycleSummary.rideCount} (${RideCalculator.formatDecimal(cycleSummary.totalDistanceKm)} km)",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = "Gross Total",
                            value = RideCalculator.formatMoney(cycleSummary.grossIncome, currencyCode),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = "Net Total",
                            value = RideCalculator.formatMoney(cycleSummary.netIncome, currencyCode),
                            highlight = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Local Excel & PDF Export + Excel Import Actions (Destination folder & browser)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val fileTag = selectedDateFilter ?: "${selectedCycle.startDateIso}_to_${selectedCycle.endDateIso}"
                                pendingExport = PendingReportExport("RideLedger_$fileTag.xlsx", isPdf = false)
                            },
                            enabled = reportGroups.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = {
                                val fileTag = selectedDateFilter ?: "${selectedCycle.startDateIso}_to_${selectedCycle.endDateIso}"
                                pendingExport = PendingReportExport("RideLedger_$fileTag.pdf", isPdf = true)
                            },
                            enabled = reportGroups.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                if (LocalStorageManager.hasValidTreePermission(context, grantedFolderUri)) {
                                    browserStartInAppFolder = false
                                    showImportBrowserDialog = true
                                } else {
                                    showImportPermissionDialog = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reports_import_excel_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            if (reportGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (hasActiveFilter) {
                                "No matching rides for current filters"
                            } else {
                                "No daily shift income for this period"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (hasActiveFilter) {
                                "Try clearing the search query, platform, payment method, or date filter."
                            } else {
                                "Logged rides are grouped by your configured shift day across the 21st–20th monthly cycle."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasActiveFilter) {
                            TextButton(
                                onClick = {
                                    onDateFilterSelected(null)
                                    onClearFilters()
                                },
                                modifier = Modifier.testTag("clear_shift_filters_button")
                            ) {
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
            } else if (isExpandedLayout) {
                val selectedGroup = reportGroups.firstOrNull { it.dateIso == expandedDayIso } ?: reportGroups.first()
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reportGroups, key = { it.dateIso }) { group ->
                            DailyReportHeaderCard(
                                group = group,
                                currencyCode = currencyCode,
                                isExpanded = group.dateIso == selectedGroup.dateIso,
                                onClick = { expandedDayIso = group.dateIso },
                                onSubmitToFinance = { shiftPendingFinance = group }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Rides on Shift ${RideCalculator.formatDisplayDate(selectedGroup.dateIso)}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(selectedGroup.rides, key = { it.id }) { ride ->
                            DetailedRideReportCard(
                                ride = ride,
                                currencyCode = currencyCode,
                                onEdit = { onEditRide(ride) },
                                onDelete = { ridePendingDelete = ride }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(reportGroups, key = { it.dateIso }) { group ->
                        val isExpanded = expandedDayIso == group.dateIso
                        Column(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                        ) {
                            DailyReportHeaderCard(
                                group = group,
                                currencyCode = currencyCode,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedDayIso = if (isExpanded) null else group.dateIso
                                },
                                onSubmitToFinance = { shiftPendingFinance = group }
                            )
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    group.rides.forEach { ride ->
                                        DetailedRideReportCard(
                                            ride = ride,
                                            currencyCode = currencyCode,
                                            onEdit = { onEditRide(ride) },
                                            onDelete = { ridePendingDelete = ride }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCalendarDialog) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showCalendarDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateFilterSelected(RideCalculator.toIsoDate(millis))
                        }
                        showCalendarDialog = false
                    }
                ) {
                    Text("Filter Day")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    ridePendingDelete?.let { ride ->
        AlertDialog(
            onDismissRequest = { ridePendingDelete = null },
            title = { Text("Delete Ride Entry?") },
            text = {
                val pickupLabel = if (ride.pickupPointName.isNotBlank()) " (${ride.pickupPointName})" else ""
                Text("Remove this ${ride.platformName}$pickupLabel ride of ${RideCalculator.formatMoney(ride.grossFare, currencyCode)}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRide(ride)
                        ridePendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ridePendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    shiftPendingFinance?.let { group ->
        AlertDialog(
            onDismissRequest = { shiftPendingFinance = null },
            title = { Text("Submit Daily Shift to Finance?") },
            text = {
                Text(
                    "Submit ${group.unsubmittedShiftRideIds.size} pending rides for shift ${RideCalculator.formatDisplayDate(group.dateIso)} " +
                        "(${RideCalculator.formatMoney(group.unsubmittedShiftGross, currencyCode)}) to Finance? " +
                        "Once submitted, these amounts will no longer show as due."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitShiftToFinance(group.unsubmittedShiftRideIds, group.dateIso)
                        shiftPendingFinance = null
                    },
                    modifier = Modifier.testTag("confirm_shift_submit_finance")
                ) {
                    Text("Submit to Finance")
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftPendingFinance = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingExport?.let { request ->
        ExportDestinationDialog(
            fileName = request.fileName,
            formatLabel = if (request.isPdf) "PDF" else "Excel",
            grantedFolderUri = grantedFolderUri,
            onDismiss = { pendingExport = null },
            onSaveToGrantedFolder = {
                val uri = grantedFolderUri
                pendingExport = null
                if (!uri.isNullOrBlank()) {
                    exportToGrantedFolder(uri, request)
                }
            },
            onChooseDestinationFolder = {
                runCatching { exportFolderPickerLauncher.launch(null) }
                    .onFailure {
                        pendingExport = null
                        onUiMessage("Folder picker unavailable on this device; saving in App 'data' folder.")
                        exportToAppDataFolder(request)
                    }
            },
            onSaveToAppDataFolder = {
                pendingExport = null
                exportToAppDataFolder(request)
            },
            onUseSystemSavePicker = {
                runCatching { systemSaveLauncher.launch(request.fileName) }
                    .onFailure {
                        pendingExport = null
                        onUiMessage("System file picker unavailable; saving in App 'data' folder.")
                        exportToAppDataFolder(request)
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
private fun SummaryStatPill(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DailyReportHeaderCard(
    group: DailyReportGroup,
    currencyCode: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onSubmitToFinance: () -> Unit
) {
    val s = group.summary
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("daily_report_card_${group.dateIso}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = RideCalculator.formatDisplayDate(group.dateIso),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${s.rideCount} rides • ${RideCalculator.formatDecimal(s.totalDistanceKm)} km hired",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = RideCalculator.formatMoney(s.netIncome, currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gross ${RideCalculator.formatMoney(s.grossIncome, currencyCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse day rides" else "Expand day rides"
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniExpenseTag("Fuel", RideCalculator.formatDecimal(s.totalFuel))
                MiniExpenseTag("Salik", RideCalculator.formatDecimal(s.totalSalik))
                MiniExpenseTag("Comm", RideCalculator.formatDecimal(s.totalCommission))
                MiniExpenseTag("Booking", RideCalculator.formatDecimal(s.totalBookingFee))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (group.isShiftSubmittedToFinance) {
                    Surface(
                        color = Emerald500.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Submitted to Finance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Amber500.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${group.unsubmittedShiftRideIds.size} rides due (${RideCalculator.formatMoney(group.unsubmittedShiftGross, currencyCode)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber500,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = onSubmitToFinance,
                        modifier = Modifier.testTag("shift_submit_finance_${group.dateIso}")
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit to Finance")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniExpenseTag(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailedRideReportCard(
    ride: RideEntity,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ride.platformName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = if (ride.pickupPointName.isNotBlank()) {
                            "${ride.pickupPointName} • ${RideCalculator.toTimeLabel(ride.rideDateMillis)}"
                        } else {
                            "${RideCalculator.toIsoDate(ride.rideDateMillis)} ${RideCalculator.toTimeLabel(ride.rideDateMillis)}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ride.paymentMethod.ifBlank { "Cash" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (ride.isSubmittedToFinance) {
                        Surface(
                            color = Emerald500.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Settled",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald500,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit ride",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete ride",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Distance: ${RideCalculator.formatDecimal(ride.distanceKm)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val fareLabel = if (RideCalculator.usesYourEarningLabel(ride.platformName)) "Your Earning" else "Gross"
                Text(
                    text = "$fareLabel: ${RideCalculator.formatMoney(ride.grossFare, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val costParts = buildList {
                    add("Fuel ${RideCalculator.formatDecimal(ride.fuelCost)}")
                    if (ride.salikCost > 0) add("Salik ${RideCalculator.formatDecimal(ride.salikCost)}")
                    if (ride.boltCommission > 0) add("Comm ${RideCalculator.formatDecimal(ride.boltCommission)}")
                    if (ride.bookingFee > 0) add("Booking ${RideCalculator.formatDecimal(ride.bookingFee)}")
                }.joinToString(" • ")

                Text(
                    text = costParts,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Net ${RideCalculator.formatMoney(ride.netIncome, currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (ride.notes.isNotBlank()) {
                Text(
                    text = "Note: ${ride.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
