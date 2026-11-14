package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.domain.RideCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    driver: DriverAccountEntity?,
    appSettings: AppSettingsEntity,
    platforms: List<PlatformEntity>,
    pickupPoints: List<PickupPointEntity>,
    onUpdateProfile: (fullName: String, mobileNumber: String) -> Unit,
    onChangePassword: (currentPin: String, newPin: String, confirmPin: String) -> Unit,
    onUpdateFuelAndCurrency: (fuelText: String, currencyCode: String) -> Unit,
    onAddPlatform: (String) -> Unit,
    onDeletePlatform: (PlatformEntity) -> Unit,
    onAddPickupPoint: (name: String, boltFeeText: String, dtcFeeText: String) -> Unit,
    onUpdatePickupPoint: (PickupPointEntity, newName: String, boltFeeText: String, dtcFeeText: String) -> Unit,
    onDeletePickupPoint: (PickupPointEntity) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullNameInput by remember(driver?.fullName) { mutableStateOf(driver?.fullName ?: "") }
    var mobileInput by remember(driver?.mobileNumber) { mutableStateOf(driver?.mobileNumber ?: "") }

    var fuelRateInput by remember(appSettings.fuelCostPerKm) {
        mutableStateOf(appSettings.fuelCostPerKm.toString())
    }
    var currencyInput by remember(appSettings.currencyCode) {
        mutableStateOf(appSettings.currencyCode)
    }

    LaunchedEffect(driver?.fullName, driver?.mobileNumber) {
        if (driver != null) {
            fullNameInput = driver.fullName
            mobileInput = driver.mobileNumber
        }
    }

    var showAddPlatformDialog by remember { mutableStateOf(false) }
    var showAddPickupDialog by remember { mutableStateOf(false) }
    var editingPickup by remember { mutableStateOf<PickupPointEntity?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

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
                    text = "Manage driver profile, fuel rate, platforms, and pickup fees",
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

        // 2. Universal Fuel Cost per km Hired Distance & Currency
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
                                text = "Applied automatically per km of hired distance on all new rides",
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

        // 3. Ride-Sharing Platforms
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ride-Sharing Platforms", style = MaterialTheme.typography.titleMedium)
                        }
                        FilledTonalButton(
                            onClick = { showAddPlatformDialog = true },
                            modifier = Modifier.testTag("settings_add_platform_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    if (platforms.isEmpty()) {
                        Text(
                            text = "No platforms added yet. Tap Add to create platforms like Bolt or DTC.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        platforms.forEachIndexed { index, platform ->
                            if (index > 0) HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = platform.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val hint = when {
                                        RideCalculator.isBoltPlatform(platform.name) -> "Supports Booking Fee & Street Hail/OTP Commission"
                                        RideCalculator.isDtcPlatform(platform.name) -> "Supports Booking Fee"
                                        else -> "Standard platform"
                                    }
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

        // 4. Pickup Points & Default Bolt/DTC Booking Fees
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddPlatformDialog) {
        AddPlatformDialog(
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
}

@Composable
private fun AddPlatformDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ride-Sharing Platform") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Platform Name") },
                placeholder = { Text("e.g. Bolt, DTC, Uber, Careem") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_platform_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
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
