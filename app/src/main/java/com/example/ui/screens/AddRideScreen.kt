package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.domain.RideCalculator
import com.example.ui.components.paymentMethodIcon
import com.example.ui.components.sleekCardBorder
import com.example.ui.theme.Emerald500
import com.example.ui.viewmodel.RideFormState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRideScreen(
    formState: RideFormState,
    platforms: List<PlatformEntity>,
    pickupPoints: List<PickupPointEntity>,
    appSettings: AppSettingsEntity,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onPlatformSelected: (String) -> Unit,
    onPickupPointSelected: (String) -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onDistanceChanged: (String) -> Unit,
    onGrossFareChanged: (String) -> Unit,
    onTipsChanged: (String) -> Unit,
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
    var showTimePicker by remember { mutableStateOf(false) }
    var platformDropdownExpanded by remember { mutableStateOf(false) }
    var pickupDropdownExpanded by remember { mutableStateOf(false) }

    val fuelCostPerKm = appSettings.fuelCostPerKm
    val currencyCode = appSettings.currencyCode
    val fuelCost = formState.computedFuelCost(fuelCostPerKm)
    val netIncome = formState.computedNetIncome(fuelCostPerKm)
    val computedShiftIso = formState.computedShiftDateIso(appSettings)
    val withinShift = formState.isWithinShift(appSettings)

    val fareInputLabel = when {
        formState.isDtcEventBooking -> "Full Fare ($currencyCode)"
        formState.usesYourEarningLabel -> "Your Earning ($currencyCode)"
        else -> "Fare ($currencyCode)"
    }
    val fareReceiptLabel = when {
        formState.isDtcEventBooking -> "Full Fare"
        formState.usesYourEarningLabel -> "Your Earning"
        else -> "Fare"
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 360.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (formState.editingRideId != null) "Edit Ride Entry" else "New Ride Entry",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter fare, tips & trip costs — net updates live",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (formState.editingRideId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
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

                // Date & Time Selectors + Shift Attribution Pill
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = sleekCardBorder(),
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .weight(1.35f)
                                .clickable { showDatePicker = true }
                                .testTag("ride_date_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Ride date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Ride Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = RideCalculator.formatDisplayDate(formState.dateIso),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = sleekCardBorder(),
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .weight(0.85f)
                                .clickable { showTimePicker = true }
                                .testTag("ride_time_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Ride time",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = RideCalculator.toTimeLabel(formState.rideHour, formState.rideMinute),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = if (withinShift) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (withinShift) {
                                "Shift Day: ${RideCalculator.formatDisplayDate(computedShiftIso)} (${RideCalculator.formatShiftWindow(appSettings)})"
                            } else {
                                "Outside shift (${RideCalculator.formatShiftWindow(appSettings)}) — counted on ${RideCalculator.formatDisplayDate(computedShiftIso)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Platform, Conditional Pickup & Payment Method Card
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = sleekCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Trip Source & Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

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

                        val visiblePickupPoints = remember(pickupPoints, formState.selectedPlatform) {
                            pickupPoints.filter {
                                RideCalculator.isPickupPointAllowedForPlatform(formState.selectedPlatform, it.name)
                            }
                        }

                        AnimatedVisibility(visible = formState.showPickupPoint) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        visiblePickupPoints.forEach { pickup ->
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

                                if (visiblePickupPoints.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        visiblePickupPoints.forEach { pickup ->
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

                        HorizontalDivider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payment Method",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            formState.allowedPaymentMethods.forEach { method ->
                                val selected = method.equals(formState.paymentMethod, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onPaymentMethodSelected(method) },
                                    label = { Text(method) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (selected) Icons.Default.Check else paymentMethodIcon(method),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("payment_method_${method.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Distance, Fare, Tips & Expenses Card
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = sleekCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (formState.isDtcEventBooking) "DTC Event Booking Fare" else "Fare, Tips & Costs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (formState.isDtcEventBooking) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "DTC Event Booking: Distance, fuel, Salik, and booking fee are skipped. Full fare equals both Fare Total and Net Income.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (isCompactWidth && !formState.isDtcEventBooking) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = formState.distanceInput,
                                    onValueChange = onDistanceChanged,
                                    label = { Text("Distance (km)") },
                                    placeholder = { Text("0.0") },
                                    leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("distance_input")
                                )
                                OutlinedTextField(
                                    value = formState.grossFareInput,
                                    onValueChange = onGrossFareChanged,
                                    label = { Text(fareInputLabel) },
                                    placeholder = { Text("0.00") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gross_fare_input")
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!formState.isDtcEventBooking) {
                                    OutlinedTextField(
                                        value = formState.distanceInput,
                                        onValueChange = onDistanceChanged,
                                        label = { Text("Distance (km)") },
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
                                }

                                OutlinedTextField(
                                    value = formState.grossFareInput,
                                    onValueChange = onGrossFareChanged,
                                    label = { Text(fareInputLabel) },
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
                        }

                        // Tips field (Bolt, DTC, Uber)
                        AnimatedVisibility(visible = formState.showTips) {
                            OutlinedTextField(
                                value = formState.tipsInput,
                                onValueChange = onTipsChanged,
                                label = { Text("Tips ($currencyCode) — Optional") },
                                placeholder = { Text("0.00") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Emerald500
                                    )
                                },
                                supportingText = {
                                    Text("Customer tip added directly to your Net Income (${formState.selectedPlatform})")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tips_input")
                            )
                        }

                        // Automatic Fuel Cost hidden for DTC Event Booking
                        AnimatedVisibility(visible = formState.showDistanceAndFuel) {
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
                        }

                        // Manual Salik hidden for DTC Event Booking and Uber
                        AnimatedVisibility(visible = formState.showSalikCost) {
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
                        }

                        // Conditional Bolt Commission
                        AnimatedVisibility(visible = formState.showBoltCommission) {
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

                        // Conditional Booking Fee (Bolt or DTC, excluded for DTC Event Booking)
                        AnimatedVisibility(visible = formState.showBookingFee) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = formState.bookingFeeInput,
                                    onValueChange = onBookingFeeChanged,
                                    label = { Text("${formState.selectedPlatform} Booking Fee ($currencyCode)") },
                                    placeholder = { Text("0.00") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    ),
                    border = sleekCardBorder(0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("net_income_preview_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Income Calculation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = paymentMethodIcon(formState.paymentMethod),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formState.paymentMethod,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        ReceiptLine(fareReceiptLabel, RideCalculator.formatMoney(formState.grossFare, currencyCode))
                        if (formState.showTips && formState.effectiveTips > 0.0) {
                            ReceiptLine(
                                label = "+ Tips",
                                value = RideCalculator.formatMoney(formState.effectiveTips, currencyCode),
                                valueColor = Emerald500
                            )
                        }
                        if (formState.showDistanceAndFuel) {
                            ReceiptLine("− Fuel Cost", RideCalculator.formatMoney(fuelCost, currencyCode))
                        }
                        if (formState.showSalikCost) {
                            ReceiptLine("− Salik Cost", RideCalculator.formatMoney(formState.effectiveSalikCost, currencyCode))
                        }
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
                                style = MaterialTheme.typography.headlineSmall,
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

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = formState.rideHour,
            initialMinute = formState.rideMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Ride Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReceiptLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
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
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
