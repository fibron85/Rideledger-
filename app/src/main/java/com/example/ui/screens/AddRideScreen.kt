package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.domain.RideCalculator
import com.example.ui.viewmodel.RideFormState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRideScreen(
    formState: RideFormState,
    platforms: List<PlatformEntity>,
    pickupPoints: List<PickupPointEntity>,
    fuelCostPerKm: Double,
    currencyCode: String,
    onDateSelected: (Long) -> Unit,
    onPlatformSelected: (String) -> Unit,
    onPickupPointSelected: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onGrossFareChanged: (String) -> Unit,
    onSalikCostChanged: (String) -> Unit,
    onBoltCommissionChanged: (String) -> Unit,
    onBookingFeeChanged: (String) -> Unit,
    onRememberBookingFeeChanged: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSubmitRide: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var platformDropdownExpanded by remember { mutableStateOf(false) }
    var pickupDropdownExpanded by remember { mutableStateOf(false) }

    val fuelCost = formState.computedFuelCost(fuelCostPerKm)
    val netIncome = formState.computedNetIncome(fuelCostPerKm)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (formState.editingRideId != null) "Edit Ride Entry" else "Input New Ride",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Enter fare & costs — net income updates automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (formState.editingRideId != null) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.testTag("cancel_edit_ride_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            }

            // Date Picker Row
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .testTag("ride_date_selector")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Ride date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ride Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = RideCalculator.formatDisplayDate(formState.dateIso),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Change")
                    }
                }
            }

            // Platform & Pickup Selection Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Trip Source", style = MaterialTheme.typography.titleMedium)

                    // Platform Dropdown + Quick Chips
                    ExposedDropdownMenuBox(
                        expanded = platformDropdownExpanded,
                        onExpandedChange = { platformDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = formState.selectedPlatform,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ride-Sharing Platform") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("platform_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = platformDropdownExpanded,
                            onDismissRequest = { platformDropdownExpanded = false }
                        ) {
                            platforms.forEach { platform ->
                                DropdownMenuItem(
                                    text = { Text(platform.name) },
                                    onClick = {
                                        onPlatformSelected(platform.name)
                                        platformDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (platforms.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            platforms.forEach { platform ->
                                val selected = platform.name.equals(formState.selectedPlatform, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onPlatformSelected(platform.name) },
                                    label = { Text(platform.name) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Pickup Point Dropdown + Quick Chips
                    ExposedDropdownMenuBox(
                        expanded = pickupDropdownExpanded,
                        onExpandedChange = { pickupDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = formState.selectedPickupPoint,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pickup Point") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickupDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("pickup_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = pickupDropdownExpanded,
                            onDismissRequest = { pickupDropdownExpanded = false }
                        ) {
                            pickupPoints.forEach { pickup ->
                                DropdownMenuItem(
                                    text = { Text(pickup.name) },
                                    onClick = {
                                        onPickupPointSelected(pickup.name)
                                        pickupDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (pickupPoints.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            pickupPoints.forEach { pickup ->
                                val selected = pickup.name.equals(formState.selectedPickupPoint, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onPickupPointSelected(pickup.name) },
                                    label = { Text(pickup.name) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Distance, Gross Fare & Expenses Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Fare & Deductions", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.distanceInput,
                            onValueChange = onDistanceChanged,
                            label = { Text("Hired Distance (km)") },
                            placeholder = { Text("0.0") },
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("distance_input")
                        )

                        OutlinedTextField(
                            value = formState.grossFareInput,
                            onValueChange = onGrossFareChanged,
                            label = { Text("Gross Fare ($currencyCode)") },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gross_fare_input")
                        )
                    }

                    // Automatic Fuel Cost Calculation Banner
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_fuel_cost_display")
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
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Fuel Cost (Auto-Calculated)",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${RideCalculator.formatDecimal(formState.distanceKm)} km × ${RideCalculator.formatMoney(fuelCostPerKm, currencyCode)}/km",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = RideCalculator.formatMoney(fuelCost, currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Salik Cost (Manual)
                    OutlinedTextField(
                        value = formState.salikCostInput,
                        onValueChange = onSalikCostChanged,
                        label = { Text("Salik Cost ($currencyCode) — Manual") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Icon(Icons.Default.Toll, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("salik_cost_input")
                    )

                    // Conditional Bolt Commission (Visible ONLY when Platform == Bolt AND Pickup is Street Hail or Bolt OTP / OTP Booking)
                    AnimatedVisibility(visible = formState.showBoltCommission) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = formState.boltCommissionInput,
                                onValueChange = onBoltCommissionChanged,
                                label = { Text("Bolt Commission ($currencyCode)") },
                                placeholder = { Text("0.00") },
                                leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
                                supportingText = {
                                    Text("Shown for Bolt rides from Street Hail or Bolt OTP booking")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bolt_commission_input")
                            )
                        }
                    }

                    // Conditional Booking Fee (Visible ONLY when Platform == Bolt or DTC)
                    AnimatedVisibility(visible = formState.showBookingFee) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formState.bookingFeeInput,
                                onValueChange = onBookingFeeChanged,
                                label = { Text("${formState.selectedPlatform} Booking Fee ($currencyCode)") },
                                placeholder = { Text("0.00") },
                                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                supportingText = {
                                    Text("Add or edit booking fee for ${formState.selectedPickupPoint}")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("booking_fee_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Save as default ${formState.selectedPlatform} fee for ${formState.selectedPickupPoint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = formState.rememberBookingFee,
                                    onCheckedChange = onRememberBookingFeeChanged,
                                    modifier = Modifier.testTag("remember_booking_fee_switch")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formState.notesInput,
                        onValueChange = onNotesChanged,
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Trip note or shift remark") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ride_notes_input")
                    )
                }
            }

            // Live Net Income Calculation Receipt Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_income_preview_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Live Income Calculation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ReceiptLine("Gross Fare", RideCalculator.formatMoney(formState.grossFare, currencyCode))
                    ReceiptLine("− Fuel Cost", RideCalculator.formatMoney(fuelCost, currencyCode))
                    ReceiptLine("− Salik Cost", RideCalculator.formatMoney(formState.salikCost, currencyCode))
                    if (formState.showBoltCommission) {
                        ReceiptLine("− Bolt Commission", RideCalculator.formatMoney(formState.effectiveCommission, currencyCode))
                    }
                    if (formState.showBookingFee) {
                        ReceiptLine("− Booking Fee (${formState.selectedPlatform})", RideCalculator.formatMoney(formState.effectiveBookingFee, currencyCode))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Income",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = RideCalculator.formatMoney(netIncome, currencyCode),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netIncome >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("net_income_value")
                        )
                    }
                }
            }

            if (formState.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onSubmitRide,
                enabled = !formState.isSubmitting,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_income_button")
            ) {
                Text(
                    text = if (formState.editingRideId != null) "Update Income" else "Submit Income",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
