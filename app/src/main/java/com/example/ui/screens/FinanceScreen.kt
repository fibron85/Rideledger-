package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.FinanceShiftSummary
import com.example.domain.FinanceUiState
import com.example.domain.RideCalculator
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Sky500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(
    financeState: FinanceUiState,
    currencyCode: String,
    onSubmitShiftToFinance: (rideIds: List<Long>, shiftDateIso: String) -> Unit,
    onSubmitAllPending: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingConfirmShift by remember { mutableStateOf<FinanceShiftSummary?>(null) }
    var showConfirmSubmitAll by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Finance & Settlement",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Track Cash on Hand, Card/InAppPaid/Account dues, and submit daily shift income to Finance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top Two Summary Cards: Cash on Hand vs Other Payment Due from Finance
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinanceMetricCard(
                        title = "CASH ON HAND",
                        amountText = RideCalculator.formatMoney(financeState.cashOnHandGross, currencyCode),
                        subText = "${financeState.cashRideCount} unsubmitted cash rides\nNet: ${RideCalculator.formatMoney(financeState.cashOnHandNet, currencyCode)}",
                        icon = Icons.Default.Payments,
                        accentColor = Amber500,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finance_cash_on_hand_card")
                    )

                    FinanceMetricCard(
                        title = "OTHER DUE FROM FINANCE",
                        amountText = RideCalculator.formatMoney(financeState.otherPaymentDueGross, currencyCode),
                        subText = "${financeState.otherPaymentRideCount} Card / InAppPaid / Account\nNet: ${RideCalculator.formatMoney(financeState.otherPaymentDueNet, currencyCode)}",
                        icon = Icons.Default.CreditCard,
                        accentColor = Sky500,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finance_other_due_card")
                    )
                }

                // Breakdown of Other Payment Due (Card, InAppPaid, Account)
                if (financeState.otherPaymentBreakdown.isNotEmpty()) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Other Payment Due Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            financeState.otherPaymentBreakdown.forEachIndexed { index, item ->
                                if (index > 0) HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.method,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${item.rideCount} unsubmitted rides • Net ${RideCalculator.formatMoney(item.netAmount, currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = RideCalculator.formatMoney(item.grossAmount, currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Sky500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pending Daily Shift Submissions Header
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pending Daily Shifts",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (financeState.pendingShifts.isEmpty()) {
                            "No payment due for Finance submission"
                        } else {
                            "Total pending: ${RideCalculator.formatMoney(financeState.totalPendingDueGross, currencyCode)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (financeState.pendingShifts.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { showConfirmSubmitAll = true },
                        modifier = Modifier.testTag("finance_submit_all_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit All Due")
                    }
                }
            }
        }

        if (financeState.pendingShifts.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .testTag("finance_no_due_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "All Daily Income Settled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Once a shift is submitted to Finance, its Cash on Hand and Other Payment Due balances clear to zero.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(financeState.pendingShifts, key = { "pending_${it.shiftDateIso}" }) { shift ->
                Box(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                ) {
                    PendingFinanceShiftCard(
                        shift = shift,
                        currencyCode = currencyCode,
                        onSubmitClick = { pendingConfirmShift = shift }
                    )
                }
            }
        }

        // Submitted Finance History
        if (financeState.submittedShifts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Submitted Finance History",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Settled shifts are no longer counted as due",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(financeState.submittedShifts, key = { "submitted_${it.shiftDateIso}" }) { shift ->
                Box(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                ) {
                    SubmittedFinanceShiftCard(
                        shift = shift,
                        currencyCode = currencyCode
                    )
                }
            }
        }
    }

    pendingConfirmShift?.let { shift ->
        AlertDialog(
            onDismissRequest = { pendingConfirmShift = null },
            title = { Text("Submit Daily Income to Finance?") },
            text = {
                Text(
                    "Confirm submission of Shift ${RideCalculator.formatDisplayDate(shift.shiftDateIso)} " +
                        "(${shift.rideCount} rides: Cash ${RideCalculator.formatMoney(shift.cashOnHandGross, currencyCode)}, " +
                        "Other ${RideCalculator.formatMoney(shift.otherDueGross, currencyCode)})? " +
                        "Once submitted, these amounts will clear from Finance due."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitShiftToFinance(shift.rideIds, shift.shiftDateIso)
                        pendingConfirmShift = null
                    },
                    modifier = Modifier.testTag("confirm_submit_shift_finance_button")
                ) {
                    Text("Submit to Finance")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmShift = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmSubmitAll) {
        AlertDialog(
            onDismissRequest = { showConfirmSubmitAll = false },
            title = { Text("Submit All Pending Shifts?") },
            text = {
                Text(
                    "Submit ${financeState.pendingShifts.size} pending shifts totaling " +
                        "${RideCalculator.formatMoney(financeState.totalPendingDueGross, currencyCode)} to Finance?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSubmitAll = false
                        onSubmitAllPending()
                    },
                    modifier = Modifier.testTag("confirm_submit_all_finance_button")
                ) {
                    Text("Submit All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSubmitAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FinanceMetricCard(
    title: String,
    amountText: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingFinanceShiftCard(
    shift: FinanceShiftSummary,
    currencyCode: String,
    onSubmitClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance_pending_shift_${shift.shiftDateIso}")
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
                        text = RideCalculator.formatDisplayDate(shift.shiftDateIso),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${shift.rideCount} rides • Net ${RideCalculator.formatMoney(shift.totalNet, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Amber500.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber500,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Cash on Hand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = RideCalculator.formatMoney(shift.cashOnHandGross, currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Other Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = RideCalculator.formatMoney(shift.otherDueGross, currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = onSubmitClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = Color(0xFF090E1A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finance_submit_shift_${shift.shiftDateIso}")
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Shift Income to Finance",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SubmittedFinanceShiftCard(
    shift: FinanceShiftSummary,
    currencyCode: String
) {
    val submittedTimeText = shift.lastSubmittedAt?.let { millis ->
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(millis))
    } ?: "Submitted"

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance_submitted_shift_${shift.shiftDateIso}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = RideCalculator.formatDisplayDate(shift.shiftDateIso),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${shift.rideCount} rides • Settled $submittedTimeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cash ${RideCalculator.formatMoney(shift.cashOnHandGross, currencyCode)} • Other ${RideCalculator.formatMoney(shift.otherDueGross, currencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = Emerald500.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Submitted",
                        style = MaterialTheme.typography.labelSmall,
                        color = Emerald500,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = RideCalculator.formatMoney(shift.totalGross, currencyCode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
