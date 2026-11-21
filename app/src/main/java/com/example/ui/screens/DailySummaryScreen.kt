package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AppSettingsEntity
import com.example.domain.BillingCycle
import com.example.domain.DailySummaryItem
import com.example.domain.PlatformDaySummary
import com.example.domain.RideCalculator
import com.example.ui.components.PlatformPillBadge
import com.example.ui.components.TipPillBadge
import com.example.ui.components.sleekCardBorder
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Sky500
import com.example.ui.viewmodel.PeriodSummary

@Composable
fun DailySummaryScreen(
    appSettings: AppSettingsEntity,
    selectedCycle: BillingCycle,
    monthTotals: PeriodSummary,
    dailyItems: List<DailySummaryItem>,
    onShiftCycle: (Int) -> Unit,
    onBackClick: () -> Unit,
    onInspectDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyCode = appSettings.currencyCode

    Column(modifier = modifier.fillMaxSize()) {
        // Sleek Top Header & Month Cycle Switcher
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("daily_summary_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Day-wise total & platform breakdown (21st–20th cycle)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Month Cycle Switcher
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = sleekCardBorder(0.45f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShiftCycle(-1) },
                            modifier = Modifier.testTag("daily_summary_prev_month")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month cycle"
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = selectedCycle.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Shift ${RideCalculator.formatShiftWindow(appSettings)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onShiftCycle(1) },
                            modifier = Modifier.testTag("daily_summary_next_month")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month cycle"
                            )
                        }
                    }
                }

                // Monthly Totals Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyMonthPill(
                        label = "Active Days",
                        value = "${dailyItems.size}d • ${monthTotals.rideCount} rides",
                        modifier = Modifier.weight(1f)
                    )
                    DailyMonthPill(
                        label = if (monthTotals.totalTips > 0.0) "Fare (+Tips)" else "Month Fare",
                        value = if (monthTotals.totalTips > 0.0) {
                            "${RideCalculator.formatMoney(monthTotals.grossIncome, currencyCode)} (+${RideCalculator.formatDecimal(monthTotals.totalTips)})"
                        } else {
                            RideCalculator.formatMoney(monthTotals.grossIncome, currencyCode)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DailyMonthPill(
                        label = "Month Net",
                        value = RideCalculator.formatMoney(monthTotals.netIncome, currencyCode),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                }
            }
        }

        if (dailyItems.isEmpty()) {
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
                        imageVector = Icons.Default.CalendarViewDay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No daily income recorded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Rides logged in ${selectedCycle.label} will appear here grouped day by day with per-platform breakdowns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val twoColGrid = maxWidth >= 880.dp
                if (twoColGrid) {
                    val paired = dailyItems.chunked(2)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(paired, key = { pair -> pair.joinToString("_") { it.dateIso } }) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 1160.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowItems.forEach { dayItem ->
                                    DailySummaryDayCard(
                                        item = dayItem,
                                        currencyCode = currencyCode,
                                        onInspectDayClick = { onInspectDayClick(dayItem.dateIso) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(dailyItems, key = { it.dateIso }) { dayItem ->
                            DailySummaryDayCard(
                                item = dayItem,
                                currencyCode = currencyCode,
                                onInspectDayClick = { onInspectDayClick(dayItem.dateIso) },
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMonthPill(
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
        border = sleekCardBorder(if (highlight) 0.7f else 0.45f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailySummaryDayCard(
    item: DailySummaryItem,
    currencyCode: String,
    onInspectDayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = item.summary

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = sleekCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.testTag("daily_summary_day_${item.dateIso}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day Header & Total Income
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = RideCalculator.formatDisplayDate(item.dateIso),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${s.rideCount} rides • ${RideCalculator.formatDecimal(s.totalDistanceKm)} km • ${item.platformBreakdowns.size} platforms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = RideCalculator.formatMoney(s.netIncome, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = if (s.totalTips > 0.0) {
                            "Fare ${RideCalculator.formatMoney(s.grossIncome, currencyCode)} (+${RideCalculator.formatDecimal(s.totalTips)})"
                        } else {
                            "Fare ${RideCalculator.formatMoney(s.grossIncome, currencyCode)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Proportional Day Split Bar (Net vs Fuel vs Fees)
            if (s.grossIncome > 0.0) {
                val base = s.grossIncome.coerceAtLeast(0.01)
                val netRatio = (s.netIncome.coerceAtLeast(0.0) / base).toFloat().coerceIn(0f, 1f)
                val fuelRatio = (s.totalFuel / base).toFloat().coerceIn(0f, 1f)
                val feesRatio = ((s.totalSalik + s.totalCommission + s.totalBookingFee) / base).toFloat().coerceIn(0f, 1f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
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
                    val otherW = (totalW - x).coerceAtMost(totalW * feesRatio)
                    if (otherW > 0f) {
                        drawRect(color = Sky500, topLeft = Offset(x, 0f), size = Size(otherW, size.height))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Platform Breakdown",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onInspectDayClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View Rides →",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Separate Breakdown Card per Platform for this Day
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.platformBreakdowns.forEach { platform ->
                    PlatformBreakdownRowCard(
                        platform = platform,
                        currencyCode = currencyCode,
                        dayIso = item.dateIso
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatformBreakdownRowCard(
    platform: PlatformDaySummary,
    currencyCode: String,
    dayIso: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sleekCardBorder(0.4f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_summary_${dayIso}_${platform.platformName}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    PlatformPillBadge(platformName = platform.platformName)
                    Text(
                        text = if (platform.distanceKm > 0.0) {
                            "${platform.rideCount} rides • ${RideCalculator.formatDecimal(platform.distanceKm)} km"
                        } else {
                            "${platform.rideCount} rides"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (platform.tips > 0.0) {
                        TipPillBadge(amountText = RideCalculator.formatDecimal(platform.tips))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = RideCalculator.formatMoney(platform.netIncome, currencyCode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val fareLabel = when {
                    RideCalculator.isDtcEventBookingPlatform(platform.platformName) -> "Full Fare"
                    RideCalculator.usesYourEarningLabel(platform.platformName) -> "Your Earning"
                    else -> "Fare"
                }
                Text(
                    text = "$fareLabel ${RideCalculator.formatMoney(platform.grossFare, currencyCode)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                val costParts = buildList {
                    if (platform.totalFuel > 0) add("Fuel ${RideCalculator.formatDecimal(platform.totalFuel)}")
                    if (platform.totalSalik > 0) add("Salik ${RideCalculator.formatDecimal(platform.totalSalik)}")
                    if (platform.totalCommission > 0) add("Comm ${RideCalculator.formatDecimal(platform.totalCommission)}")
                    if (platform.totalBookingFee > 0) add("Book ${RideCalculator.formatDecimal(platform.totalBookingFee)}")
                }.joinToString(" • ").ifBlank { "No deductions" }
                Text(
                    text = costParts,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
