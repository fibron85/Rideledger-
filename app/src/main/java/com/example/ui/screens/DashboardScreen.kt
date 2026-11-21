package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.FinanceUiState
import com.example.domain.RideCalculator
import com.example.ui.components.PaymentMethodSymbolBadge
import com.example.ui.components.PlatformPillBadge
import com.example.ui.components.TipPillBadge
import com.example.ui.components.sleekCardBorder
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Sky500
import com.example.ui.viewmodel.DailyChartPoint
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.PeriodSummary
import com.example.ui.viewmodel.PlatformSummary
import com.example.util.InAppReminderPrompt
import com.example.util.RideReminderScheduler

@Composable
fun DashboardScreen(
    driver: DriverAccountEntity?,
    dashboardState: DashboardUiState,
    financeState: FinanceUiState,
    currencyCode: String,
    fuelRatePerKm: Double,
    remindersEnabled: Boolean,
    activeInAppReminder: InAppReminderPrompt?,
    activeFinanceInAppReminder: InAppReminderPrompt?,
    onDismissInAppReminder: (String) -> Unit,
    onDismissFinanceInAppReminder: (String) -> Unit,
    onAddRideClick: () -> Unit,
    onViewReportsClick: () -> Unit,
    onOpenDailySummaryClick: () -> Unit,
    onOpenFinanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onUiMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(RideReminderScheduler.hasNotificationPermission(context))
    }

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
        hasNotificationPermission = granted || RideReminderScheduler.hasNotificationPermission(context)
        if (granted) {
            RideReminderScheduler.scheduleDailyReminders(context)
        } else {
            onUiMessage("Enable notifications in Android Settings to receive 4:00 PM & 4:00 AM alerts.")
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_driver_header),
                    contentDescription = "Night shift driver dashboard header",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x55090E1A), Color(0xF0090E1A))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLogoutConfirm = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xAA0F172A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("dashboard_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!driver?.fullName.isNullOrBlank()) {
                                    "Hello, ${driver?.fullName}"
                                } else {
                                    "Driver #${driver?.driverId ?: "------"}"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID ${driver?.driverId ?: ""} • Shift ${RideCalculator.formatDisplayDate(dashboardState.todayShiftIso)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAddRideClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald500,
                                contentColor = Color(0xFF090E1A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("dashboard_add_ride_button")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Ride", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Daily & Monthly Core Summaries
        item {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
            ) {
                val twoColHero = maxWidth >= 680.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (activeInAppReminder != null) {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            border = sleekCardBorder(0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_in_app_reminder_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Ride reminder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeInAppReminder.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = activeInAppReminder.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                ) {
                                    TextButton(
                                        onClick = { onDismissInAppReminder(activeInAppReminder.slotKey) },
                                        modifier = Modifier.testTag("dashboard_dismiss_reminder_button")
                                    ) {
                                        Text("Dismiss")
                                    }
                                    Button(
                                        onClick = {
                                            onDismissInAppReminder(activeInAppReminder.slotKey)
                                            onAddRideClick()
                                        },
                                        modifier = Modifier.testTag("dashboard_reminder_add_ride_button")
                                    ) {
                                        Text("Log Ride")
                                    }
                                }
                            }
                        }
                    }

                    if (activeFinanceInAppReminder != null) {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = Amber500.copy(alpha = 0.14f)
                            ),
                            border = sleekCardBorder(0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_finance_reminder_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Finance payment reminder",
                                        tint = Amber500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeFinanceInAppReminder.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = activeFinanceInAppReminder.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                ) {
                                    TextButton(
                                        onClick = { onDismissFinanceInAppReminder(activeFinanceInAppReminder.slotKey) },
                                        modifier = Modifier.testTag("dashboard_dismiss_finance_reminder_button")
                                    ) {
                                        Text("Dismiss")
                                    }
                                    Button(
                                        onClick = {
                                            onDismissFinanceInAppReminder(activeFinanceInAppReminder.slotKey)
                                            onOpenFinanceClick()
                                        },
                                        modifier = Modifier.testTag("dashboard_reminder_open_finance_button")
                                    ) {
                                        Text("Submit Finance")
                                    }
                                }
                            }
                        }
                    }

                    if (remindersEnabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = sleekCardBorder(0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        tint = Amber500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Allow notifications for daily 4:00 PM & 4:00 AM ride reminders",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        runCatching {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }.onFailure {
                                            if (!RideReminderScheduler.openAppNotificationSettings(context)) {
                                                onUiMessage("Open Android Settings → Apps → RideLedger → Notifications.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("dashboard_allow_notifications_button")
                                ) {
                                    Text("Allow")
                                }
                            }
                        }
                    }

                    if (twoColHero) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            IncomeHeroCard(
                                title = "Daily Shift Income",
                                subtitle = "${dashboardState.todaySummary.rideCount} rides • ${RideCalculator.formatDecimal(dashboardState.todaySummary.totalDistanceKm)} km • ${dashboardState.shiftWindowLabel}",
                                icon = Icons.Default.Today,
                                summary = dashboardState.todaySummary,
                                currencyCode = currencyCode,
                                accentColor = MaterialTheme.colorScheme.primary,
                                testTag = "daily_summary_card",
                                modifier = Modifier.weight(1f)
                            )
                            IncomeHeroCard(
                                title = "Monthly (${dashboardState.currentBillingCycle.label})",
                                subtitle = "${dashboardState.monthSummary.rideCount} rides • ${RideCalculator.formatDecimal(dashboardState.monthSummary.totalDistanceKm)} km • 21st–20th",
                                icon = Icons.Default.CalendarMonth,
                                summary = dashboardState.monthSummary,
                                currencyCode = currencyCode,
                                accentColor = Sky500,
                                testTag = "monthly_summary_card",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        IncomeHeroCard(
                            title = "Daily Shift Income",
                            subtitle = "${dashboardState.todaySummary.rideCount} rides • ${RideCalculator.formatDecimal(dashboardState.todaySummary.totalDistanceKm)} km • Shift ${dashboardState.shiftWindowLabel}",
                            icon = Icons.Default.Today,
                            summary = dashboardState.todaySummary,
                            currencyCode = currencyCode,
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "daily_summary_card"
                        )
                        IncomeHeroCard(
                            title = "Monthly Income (${dashboardState.currentBillingCycle.label})",
                            subtitle = "${dashboardState.monthSummary.rideCount} rides • ${RideCalculator.formatDecimal(dashboardState.monthSummary.totalDistanceKm)} km • 21st–20th cycle",
                            icon = Icons.Default.CalendarMonth,
                            summary = dashboardState.monthSummary,
                            currencyCode = currencyCode,
                            accentColor = Sky500,
                            testTag = "monthly_summary_card"
                        )
                    }

                    // Active Fuel Rate & Shift Window Chip Strip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = sleekCardBorder(0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = Amber500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Universal Fuel Rate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${RideCalculator.formatMoney(fuelRatePerKm, currencyCode)} / km",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Sky500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Configured Shift Timing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = dashboardState.shiftWindowLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Pending Finance Quick Strip
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = sleekCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Finance Balance Due",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Cash on Hand: ${RideCalculator.formatMoney(financeState.cashOnHandGross, currencyCode)} • Other Due: ${RideCalculator.formatMoney(financeState.otherPaymentDueGross, currencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onOpenFinanceClick,
                            modifier = Modifier.testTag("dashboard_open_finance_button")
                        ) {
                            Text("Finance")
                        }
                    }
                }

                // Daily Summary Quick Strip
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = sleekCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarViewDay,
                                    contentDescription = null,
                                    tint = Sky500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Daily Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Day-wise income & separate platform breakdown (${dashboardState.currentBillingCycle.label})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onOpenDailySummaryClick,
                            modifier = Modifier.testTag("dashboard_daily_summary_button")
                        ) {
                            Text("Summary")
                        }
                    }
                }

                // Monthly Net Income Trend Chart
                if (dashboardState.monthlyChartPoints.isNotEmpty()) {
                    MonthlyTrendChartCard(
                        points = dashboardState.monthlyChartPoints,
                        currencyCode = currencyCode
                    )
                }

                // Platform Breakdown Card
                if (dashboardState.platformBreakdown.isNotEmpty()) {
                    PlatformBreakdownCard(
                        breakdown = dashboardState.platformBreakdown,
                        currencyCode = currencyCode
                    )
                }

                // Today's Shift Rides Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Shift Rides",
                        style = MaterialTheme.typography.titleLarge
                    )
                    FilledTonalButton(
                        onClick = onViewReportsClick,
                        modifier = Modifier.testTag("dashboard_view_reports_button")
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reports & Export")
                    }
                }
            }
            }
        }

        if (dashboardState.todayRides.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No rides logged for this shift yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap \"New Ride\" to log a trip. Fuel, commission, booking fees, shift day, and net income calculate automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(dashboardState.todayRides, key = { it.id }) { ride ->
                Box(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    RideSummaryRowCard(ride = ride, currencyCode = currencyCode)
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Sign Out of RideLedger?") },
            text = {
                Text("Your local rides and settings stay safely saved on this device for Driver #${driver?.driverId ?: ""}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun IncomeHeroCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    summary: PeriodSummary,
    currencyCode: String,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sleekCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = sleekCardBorder(0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (summary.totalTips > 0.0) "FARE (+TIPS)" else "TOTAL FARE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = RideCalculator.formatMoney(summary.grossIncome, currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (summary.totalTips > 0.0) {
                            Text(
                                text = "+${RideCalculator.formatMoney(summary.totalTips, currencyCode)} tips",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald500,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = sleekCardBorder(0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "NET INCOME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = RideCalculator.formatMoney(summary.netIncome, currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (summary.grossIncome > 0.0) {
                val gross = summary.grossIncome.coerceAtLeast(0.01)
                val netRatio = (summary.netIncome.coerceAtLeast(0.0) / gross).toFloat().coerceIn(0f, 1f)
                val fuelRatio = (summary.totalFuel / gross).toFloat().coerceIn(0f, 1f)
                val otherCostRatio = ((summary.totalSalik + summary.totalCommission + summary.totalBookingFee) / gross).toFloat().coerceIn(0f, 1f)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        val totalW = size.width
                        drawRect(color = Color(0x2264748B), size = size)
                        var x = 0f
                        val netW = totalW * netRatio
                        if (netW > 0f) {
                            drawRect(color = Emerald500, topLeft = Offset(x, 0f), size = Size(netW, size.height))
                            x += netW
                        }
                        val fuelW = totalW * fuelRatio
                        if (fuelW > 0f) {
                            drawRect(color = Amber500, topLeft = Offset(x, 0f), size = Size(fuelW, size.height))
                            x += fuelW
                        }
                        val otherW = (totalW - x).coerceAtMost(totalW * otherCostRatio)
                        if (otherW > 0f) {
                            drawRect(color = Sky500, topLeft = Offset(x, 0f), size = Size(otherW, size.height))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fuel: ${RideCalculator.formatMoney(summary.totalFuel, currencyCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "Fees/Comm: ${RideCalculator.formatMoney(summary.totalSalik + summary.totalCommission + summary.totalBookingFee, currencyCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendChartCard(
    points: List<DailyChartPoint>,
    currencyCode: String
) {
    val maxGross = points.maxOfOrNull { maxOf(it.grossIncome, it.netIncome) }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sleekCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
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
                Text("Cycle Net vs Gross Trend", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = Color(0xFF94A3B8), label = "Gross")
                    LegendDot(color = Emerald500, label = "Net")
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val count = points.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.36f).coerceIn(6f, 22f)
                val gap = (barWidth * 0.2f)

                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val grossH = ((pt.grossIncome / maxGross).toFloat().coerceIn(0f, 1f)) * (size.height - 8f)
                    val netH = ((pt.netIncome.coerceAtLeast(0.0) / maxGross).toFloat().coerceIn(0f, 1f)) * (size.height - 8f)

                    drawRoundRect(
                        color = Color(0xFF94A3B8).copy(alpha = 0.55f),
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - grossH),
                        size = Size(barWidth, grossH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Emerald500,
                        topLeft = Offset(centerX + gap / 2f, size.height - netH),
                        size = Size(barWidth, netH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val first = points.first()
                val best = points.maxByOrNull { it.netIncome }
                Text(
                    text = "Shifts active: ${points.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (best != null) {
                    Text(
                        text = "Best shift (${best.dateIso.takeLast(2)}): ${RideCalculator.formatMoney(best.netIncome, currencyCode)} net",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = first.dateIso,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlatformBreakdownCard(
    breakdown: List<PlatformSummary>,
    currencyCode: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sleekCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Cycle Breakdown by Platform", style = MaterialTheme.typography.titleMedium)
            breakdown.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.platformName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        val platformFareLabel = when {
                            RideCalculator.isDtcEventBookingPlatform(item.platformName) -> "Full Fare"
                            RideCalculator.usesYourEarningLabel(item.platformName) -> "Your Earning"
                            else -> "Fare"
                        }
                        Text(
                            text = "${item.rideCount} rides • $platformFareLabel ${RideCalculator.formatMoney(item.grossIncome, currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = RideCalculator.formatMoney(item.netIncome, currencyCode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RideSummaryRowCard(
    ride: RideEntity,
    currencyCode: String
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sleekCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlatformPillBadge(platformName = ride.platformName)
                    if (ride.pickupPointName.isNotBlank()) {
                        Text(
                            text = ride.pickupPointName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    PaymentMethodSymbolBadge(paymentMethod = ride.paymentMethod)
                    if (ride.tips > 0.0) {
                        TipPillBadge(amountText = RideCalculator.formatDecimal(ride.tips))
                    }
                }
                val farePrefix = when {
                    RideCalculator.isDtcEventBookingPlatform(ride.platformName) -> "Full Fare"
                    RideCalculator.usesYourEarningLabel(ride.platformName) -> "Your Earning"
                    else -> "Fare"
                }
                Text(
                    text = if (ride.distanceKm > 0.0) {
                        "${RideCalculator.toTimeLabel(ride.rideDateMillis)} • ${RideCalculator.formatDecimal(ride.distanceKm)} km • $farePrefix ${RideCalculator.formatMoney(ride.grossFare, currencyCode)}"
                    } else {
                        "${RideCalculator.toTimeLabel(ride.rideDateMillis)} • $farePrefix ${RideCalculator.formatMoney(ride.grossFare, currencyCode)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = RideCalculator.formatMoney(ride.netIncome, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (ride.netIncome >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
                Text(
                    text = "Net",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
