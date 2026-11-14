package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.RideEntity
import com.example.domain.RideCalculator
import com.example.ui.viewmodel.DailyReportGroup
import com.example.ui.viewmodel.PeriodSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    selectedMonthPrefix: String,
    selectedDateFilter: String?,
    monthSummary: PeriodSummary,
    reportGroups: List<DailyReportGroup>,
    currencyCode: String,
    onShiftMonth: (Int) -> Unit,
    onDateFilterSelected: (String?) -> Unit,
    onEditRide: (RideEntity) -> Unit,
    onDeleteRide: (RideEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendarDialog by remember { mutableStateOf(false) }
    var ridePendingDelete by remember { mutableStateOf<RideEntity?>(null) }
    var expandedDayIso by remember(reportGroups) {
        mutableStateOf(reportGroups.firstOrNull()?.dateIso)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 840.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Month / Date Filter Bar
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onShiftMonth(-1) },
                                modifier = Modifier.testTag("report_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            Text(
                                text = RideCalculator.formatMonthLabel(selectedMonthPrefix),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onShiftMonth(1) },
                                modifier = Modifier.testTag("report_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    // Period totals summary bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatPill(
                            label = "Rides",
                            value = "${monthSummary.rideCount} (${RideCalculator.formatDecimal(monthSummary.totalDistanceKm)} km)",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = "Gross Total",
                            value = RideCalculator.formatMoney(monthSummary.grossIncome, currencyCode),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = "Net Total",
                            value = RideCalculator.formatMoney(monthSummary.netIncome, currencyCode),
                            highlight = true,
                            modifier = Modifier.weight(1f)
                        )
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
                            text = "No daily reports for this period",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Submitted rides will automatically appear grouped day-by-day with full cost breakdowns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                onClick = { expandedDayIso = group.dateIso }
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
                                text = "Rides on ${RideCalculator.formatDisplayDate(selectedGroup.dateIso)}",
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
                                }
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
                Text("Remove this ${ride.platformName} (${ride.pickupPointName}) ride of ${RideCalculator.formatMoney(ride.grossFare, currencyCode)} from ${ride.rideDateIso}?")
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
    onClick: () -> Unit
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
                Column {
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
                        text = ride.pickupPointName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
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
                Text(
                    text = "Gross: ${RideCalculator.formatMoney(ride.grossFare, currencyCode)}",
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
