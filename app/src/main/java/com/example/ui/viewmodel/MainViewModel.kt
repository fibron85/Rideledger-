package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.PickupPointEntity
import com.example.data.local.entity.PlatformEntity
import com.example.data.local.entity.RideEntity
import com.example.data.repository.RideLedgerRepository
import com.example.domain.RideCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PeriodSummary(
    val grossIncome: Double = 0.0,
    val netIncome: Double = 0.0,
    val totalFuel: Double = 0.0,
    val totalSalik: Double = 0.0,
    val totalCommission: Double = 0.0,
    val totalBookingFee: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val rideCount: Int = 0
) {
    val totalCosts: Double
        get() = RideCalculator.round2(totalFuel + totalSalik + totalCommission + totalBookingFee)
}

data class DailyChartPoint(
    val dayOfMonth: Int,
    val dateIso: String,
    val grossIncome: Double,
    val netIncome: Double
)

data class PlatformSummary(
    val platformName: String,
    val rideCount: Int,
    val grossIncome: Double,
    val netIncome: Double
)

data class DashboardUiState(
    val todayIso: String = RideCalculator.currentIsoDate(),
    val currentMonthPrefix: String = RideCalculator.currentMonthPrefix(),
    val todaySummary: PeriodSummary = PeriodSummary(),
    val monthSummary: PeriodSummary = PeriodSummary(),
    val todayRides: List<RideEntity> = emptyList(),
    val monthlyChartPoints: List<DailyChartPoint> = emptyList(),
    val platformBreakdown: List<PlatformSummary> = emptyList()
)

data class DailyReportGroup(
    val dateIso: String,
    val summary: PeriodSummary,
    val rides: List<RideEntity>
)

data class RideFormState(
    val editingRideId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val dateIso: String = RideCalculator.currentIsoDate(),
    val selectedPlatform: String = "",
    val selectedPickupPoint: String = "",
    val distanceInput: String = "",
    val grossFareInput: String = "",
    val salikCostInput: String = "",
    val boltCommissionInput: String = "",
    val bookingFeeInput: String = "",
    val rememberBookingFee: Boolean = true,
    val notesInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val distanceKm: Double get() = distanceInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val grossFare: Double get() = grossFareInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val salikCost: Double get() = salikCostInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rawCommission: Double get() = boltCommissionInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rawBookingFee: Double get() = bookingFeeInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val showBoltCommission: Boolean
        get() = RideCalculator.showsBoltCommission(selectedPlatform, selectedPickupPoint)

    val showBookingFee: Boolean
        get() = RideCalculator.showsBookingFee(selectedPlatform)

    val effectiveCommission: Double
        get() = if (showBoltCommission) rawCommission else 0.0

    val effectiveBookingFee: Double
        get() = if (showBookingFee) rawBookingFee else 0.0

    fun computedFuelCost(fuelCostPerKm: Double): Double =
        RideCalculator.calculateFuelCost(distanceKm, fuelCostPerKm)

    fun computedNetIncome(fuelCostPerKm: Double): Double =
        RideCalculator.calculateNetIncome(
            grossFare = grossFare,
            fuelCost = computedFuelCost(fuelCostPerKm),
            salikCost = salikCost,
            boltCommission = effectiveCommission,
            bookingFee = effectiveBookingFee
        )
}

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data object RideSavedNavigateBack : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: RideLedgerRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val appSettings: StateFlow<AppSettingsEntity> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val platforms: StateFlow<List<PlatformEntity>> = repository.platforms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pickupPoints: StateFlow<List<PickupPointEntity>> = repository.pickupPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDriver: StateFlow<DriverAccountEntity?> = repository.activeDriverId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null) else repository.observeDriver(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allRides: StateFlow<List<RideEntity>> = repository.activeDriverId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList()) else repository.observeAllRides(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardState: StateFlow<DashboardUiState> = allRides
        .combine(flowOf(Unit)) { rides, _ ->
            val todayIso = RideCalculator.currentIsoDate()
            val monthPrefix = RideCalculator.currentMonthPrefix()

            val todayRides = rides.filter { it.rideDateIso == todayIso }
            val monthRides = rides.filter { it.rideDateIso.startsWith(monthPrefix) }

            val chartPoints = monthRides
                .groupBy { it.rideDateIso }
                .entries
                .sortedBy { it.key }
                .map { (dateIso, dayRides) ->
                    val dayNum = dateIso.takeLast(2).toIntOrNull() ?: 1
                    DailyChartPoint(
                        dayOfMonth = dayNum,
                        dateIso = dateIso,
                        grossIncome = RideCalculator.round2(dayRides.sumOf { it.grossFare }),
                        netIncome = RideCalculator.round2(dayRides.sumOf { it.netIncome })
                    )
                }

            val platformBreakdown = monthRides
                .groupBy { it.platformName }
                .entries
                .map { (platform, pRides) ->
                    PlatformSummary(
                        platformName = platform,
                        rideCount = pRides.size,
                        grossIncome = RideCalculator.round2(pRides.sumOf { it.grossFare }),
                        netIncome = RideCalculator.round2(pRides.sumOf { it.netIncome })
                    )
                }
                .sortedByDescending { it.netIncome }

            DashboardUiState(
                todayIso = todayIso,
                currentMonthPrefix = monthPrefix,
                todaySummary = aggregateRides(todayRides),
                monthSummary = aggregateRides(monthRides),
                todayRides = todayRides,
                monthlyChartPoints = chartPoints,
                platformBreakdown = platformBreakdown
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private val _selectedReportMonth = MutableStateFlow(RideCalculator.currentMonthPrefix())
    val selectedReportMonth: StateFlow<String> = _selectedReportMonth

    private val _selectedReportDateFilter = MutableStateFlow<String?>(null)
    val selectedReportDateFilter: StateFlow<String?> = _selectedReportDateFilter

    val reportGroups: StateFlow<List<DailyReportGroup>> = combine(
        allRides,
        _selectedReportMonth,
        _selectedReportDateFilter
    ) { rides, monthPrefix, dateFilter ->
        val filtered = if (dateFilter != null) {
            rides.filter { it.rideDateIso == dateFilter }
        } else {
            rides.filter { it.rideDateIso.startsWith(monthPrefix) }
        }
        filtered
            .groupBy { it.rideDateIso }
            .entries
            .sortedByDescending { it.key }
            .map { (dateIso, dayRides) ->
                DailyReportGroup(
                    dateIso = dateIso,
                    summary = aggregateRides(dayRides),
                    rides = dayRides.sortedByDescending { it.createdAt }
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMonthSummary: StateFlow<PeriodSummary> = combine(
        allRides,
        _selectedReportMonth,
        _selectedReportDateFilter
    ) { rides, monthPrefix, dateFilter ->
        val filtered = if (dateFilter != null) {
            rides.filter { it.rideDateIso == dateFilter }
        } else {
            rides.filter { it.rideDateIso.startsWith(monthPrefix) }
        }
        aggregateRides(filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    private val _rideFormState = MutableStateFlow(RideFormState())
    val rideFormState: StateFlow<RideFormState> = _rideFormState

    init {
        viewModelScope.launch {
            combine(platforms, pickupPoints) { platformList, pickupList ->
                platformList to pickupList
            }.collect { (platformList, pickupList) ->
                _rideFormState.update { current ->
                    var updated = current
                    if (updated.selectedPlatform.isBlank() && platformList.isNotEmpty()) {
                        updated = updated.copy(selectedPlatform = platformList.first().name)
                    }
                    if (updated.selectedPickupPoint.isBlank() && pickupList.isNotEmpty()) {
                        updated = updated.copy(selectedPickupPoint = pickupList.first().name)
                        updated = applyDefaultBookingFeeIfEmpty(updated, pickupList)
                    }
                    updated
                }
            }
        }
    }

    private fun applyDefaultBookingFeeIfEmpty(
        state: RideFormState,
        pickupList: List<PickupPointEntity>,
        forceOverwrite: Boolean = false
    ): RideFormState {
        if (!RideCalculator.showsBookingFee(state.selectedPlatform)) {
            return if (forceOverwrite) state.copy(bookingFeeInput = "") else state
        }
        if (!forceOverwrite && state.bookingFeeInput.isNotBlank()) return state
        val pickup = pickupList.firstOrNull {
            it.name.equals(state.selectedPickupPoint, ignoreCase = true)
        } ?: return state
        val defaultFee = when {
            RideCalculator.isBoltPlatform(state.selectedPlatform) -> pickup.boltBookingFee
            RideCalculator.isDtcPlatform(state.selectedPlatform) -> pickup.dtcBookingFee
            else -> 0.0
        }
        return state.copy(
            bookingFeeInput = if (defaultFee > 0.0) formatEditableNumber(defaultFee) else ""
        )
    }

    private fun formatEditableNumber(value: Double): String {
        val rounded = RideCalculator.round2(value)
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }

    fun onRideDateSelected(millis: Long) {
        _rideFormState.update {
            it.copy(
                dateMillis = millis,
                dateIso = RideCalculator.toIsoDate(millis),
                errorMessage = null
            )
        }
    }

    fun onPlatformSelected(platformName: String) {
        val pickupList = pickupPoints.value
        _rideFormState.update { current ->
            val withPlatform = current.copy(
                selectedPlatform = platformName,
                errorMessage = null
            )
            applyDefaultBookingFeeIfEmpty(withPlatform, pickupList, forceOverwrite = true)
        }
    }

    fun onPickupPointSelected(pickupName: String) {
        val pickupList = pickupPoints.value
        _rideFormState.update { current ->
            val withPickup = current.copy(
                selectedPickupPoint = pickupName,
                errorMessage = null
            )
            applyDefaultBookingFeeIfEmpty(withPickup, pickupList, forceOverwrite = true)
        }
    }

    fun onDistanceChanged(value: String) {
        _rideFormState.update { it.copy(distanceInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onGrossFareChanged(value: String) {
        _rideFormState.update { it.copy(grossFareInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onSalikCostChanged(value: String) {
        _rideFormState.update { it.copy(salikCostInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onBoltCommissionChanged(value: String) {
        _rideFormState.update { it.copy(boltCommissionInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onBookingFeeChanged(value: String) {
        _rideFormState.update { it.copy(bookingFeeInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onRememberBookingFeeChanged(remember: Boolean) {
        _rideFormState.update { it.copy(rememberBookingFee = remember) }
    }

    fun onNotesChanged(value: String) {
        _rideFormState.update { it.copy(notesInput = value.take(120)) }
    }

    fun startEditingRide(ride: RideEntity) {
        _rideFormState.value = RideFormState(
            editingRideId = ride.id,
            dateMillis = ride.rideDateMillis,
            dateIso = ride.rideDateIso,
            selectedPlatform = ride.platformName,
            selectedPickupPoint = ride.pickupPointName,
            distanceInput = formatEditableNumber(ride.distanceKm),
            grossFareInput = formatEditableNumber(ride.grossFare),
            salikCostInput = if (ride.salikCost > 0.0) formatEditableNumber(ride.salikCost) else "",
            boltCommissionInput = if (ride.boltCommission > 0.0) formatEditableNumber(ride.boltCommission) else "",
            bookingFeeInput = if (ride.bookingFee > 0.0) formatEditableNumber(ride.bookingFee) else "",
            rememberBookingFee = false,
            notesInput = ride.notes
        )
    }

    fun resetRideForm() {
        val defaultPlatform = platforms.value.firstOrNull()?.name ?: ""
        val defaultPickup = pickupPoints.value.firstOrNull()?.name ?: ""
        val initial = RideFormState(
            selectedPlatform = defaultPlatform,
            selectedPickupPoint = defaultPickup
        )
        _rideFormState.value = applyDefaultBookingFeeIfEmpty(initial, pickupPoints.value, forceOverwrite = true)
    }

    fun submitRide() {
        val state = _rideFormState.value
        val driver = activeDriver.value
        if (driver == null) {
            _rideFormState.update { it.copy(errorMessage = "Please sign in first.") }
            return
        }
        if (state.selectedPlatform.isBlank()) {
            _rideFormState.update { it.copy(errorMessage = "Please add and select a ride-sharing platform.") }
            return
        }
        if (state.selectedPickupPoint.isBlank()) {
            _rideFormState.update { it.copy(errorMessage = "Please add and select a pickup point.") }
            return
        }
        val distanceVal = state.distanceInput.toDoubleOrNull()
        if (distanceVal == null || distanceVal < 0.0) {
            _rideFormState.update { it.copy(errorMessage = "Enter a valid hired distance (km).") }
            return
        }
        val grossVal = state.grossFareInput.toDoubleOrNull()
        if (grossVal == null || grossVal <= 0.0) {
            _rideFormState.update { it.copy(errorMessage = "Enter a valid Gross Fare greater than 0.") }
            return
        }

        val settings = appSettings.value
        val fuelRate = settings.fuelCostPerKm
        val fuelCost = state.computedFuelCost(fuelRate)
        val salikCost = RideCalculator.round2(state.salikCost)
        val boltCommission = RideCalculator.round2(state.effectiveCommission)
        val bookingFee = RideCalculator.round2(state.effectiveBookingFee)
        val netIncome = RideCalculator.calculateNetIncome(
            grossFare = grossVal,
            fuelCost = fuelCost,
            salikCost = salikCost,
            boltCommission = boltCommission,
            bookingFee = bookingFee
        )

        val wasEditing = state.editingRideId != null

        viewModelScope.launch {
            _rideFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val existingCreatedAt = state.editingRideId?.let { repository.getRideById(it)?.createdAt }
            val entity = RideEntity(
                id = state.editingRideId ?: 0L,
                driverId = driver.driverId,
                rideDateMillis = state.dateMillis,
                rideDateIso = state.dateIso,
                platformName = state.selectedPlatform,
                pickupPointName = state.selectedPickupPoint,
                distanceKm = RideCalculator.round2(distanceVal),
                grossFare = RideCalculator.round2(grossVal),
                fuelCostPerKmApplied = fuelRate,
                fuelCost = fuelCost,
                salikCost = salikCost,
                boltCommission = boltCommission,
                bookingFee = bookingFee,
                netIncome = netIncome,
                notes = state.notesInput.trim(),
                createdAt = existingCreatedAt ?: System.currentTimeMillis()
            )
            repository.saveRide(entity)

            if (state.showBookingFee && state.rememberBookingFee) {
                repository.saveBookingFeeForPickupPoint(
                    pickupPointName = state.selectedPickupPoint,
                    platformName = state.selectedPlatform,
                    bookingFee = bookingFee
                )
            }

            // Reset numeric inputs while keeping date & platform for rapid next entry
            val refreshedPickups = pickupPoints.first()
            val resetState = RideFormState(
                dateMillis = state.dateMillis,
                dateIso = state.dateIso,
                selectedPlatform = state.selectedPlatform,
                selectedPickupPoint = state.selectedPickupPoint,
                rememberBookingFee = state.rememberBookingFee
            )
            _rideFormState.value = applyDefaultBookingFeeIfEmpty(
                resetState,
                refreshedPickups,
                forceOverwrite = true
            )

            _events.emit(
                UiEvent.ShowMessage(
                    if (wasEditing) "Ride updated successfully." else "Ride income saved locally."
                )
            )
            if (wasEditing) {
                _events.emit(UiEvent.RideSavedNavigateBack)
            }
        }
    }

    fun deleteRide(ride: RideEntity) {
        viewModelScope.launch {
            repository.deleteRide(ride)
            _events.emit(UiEvent.ShowMessage("Ride entry deleted."))
        }
    }

    fun shiftReportMonth(deltaMonths: Int) {
        _selectedReportDateFilter.value = null
        val current = _selectedReportMonth.value
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val parsed = sdf.parse(current)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
        }
        cal.add(Calendar.MONTH, deltaMonths)
        _selectedReportMonth.value = RideCalculator.toMonthPrefix(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH)
        )
    }

    fun setReportDateFilter(dateIso: String?) {
        _selectedReportDateFilter.value = dateIso
        if (dateIso != null && dateIso.length >= 7) {
            _selectedReportMonth.value = dateIso.substring(0, 7)
        }
    }

    fun addPlatform(name: String) {
        viewModelScope.launch {
            repository.addPlatform(name).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Platform \"$name\" added.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Failed to add platform.")) }
            )
        }
    }

    fun deletePlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            repository.deletePlatform(platform)
            _events.emit(UiEvent.ShowMessage("Platform \"${platform.name}\" removed."))
        }
    }

    fun addPickupPoint(name: String, boltFeeText: String, dtcFeeText: String) {
        val boltFee = boltFeeText.toDoubleOrNull() ?: 0.0
        val dtcFee = dtcFeeText.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            repository.addPickupPoint(name, boltFee, dtcFee).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Pickup point \"$name\" added.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Failed to add pickup point.")) }
            )
        }
    }

    fun updatePickupPoint(pickupPoint: PickupPointEntity, newName: String, boltFeeText: String, dtcFeeText: String) {
        val boltFee = boltFeeText.toDoubleOrNull() ?: 0.0
        val dtcFee = dtcFeeText.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            repository.updatePickupPoint(
                pickupPoint.copy(
                    name = newName,
                    boltBookingFee = boltFee,
                    dtcBookingFee = dtcFee
                )
            ).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Pickup point updated.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Failed to update pickup point.")) }
            )
        }
    }

    fun deletePickupPoint(pickupPoint: PickupPointEntity) {
        viewModelScope.launch {
            repository.deletePickupPoint(pickupPoint)
            _events.emit(UiEvent.ShowMessage("Pickup point \"${pickupPoint.name}\" removed."))
        }
    }

    fun updateFuelAndCurrency(fuelText: String, currencyCode: String) {
        val fuelRate = fuelText.toDoubleOrNull()
        if (fuelRate == null || fuelRate < 0.0) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Enter a valid fuel cost per km."))
            }
            return
        }
        viewModelScope.launch {
            repository.updateFuelAndCurrency(fuelRate, currencyCode).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Fuel rate & currency saved.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Save failed.")) }
            )
        }
    }

    fun updateProfile(fullName: String, mobileNumber: String) {
        val driver = activeDriver.value ?: return
        viewModelScope.launch {
            repository.updateProfile(driver.driverId, fullName, mobileNumber).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Driver profile updated.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Profile update failed.")) }
            )
        }
    }

    fun changePassword(currentPin: String, newPin: String, confirmPin: String) {
        val driver = activeDriver.value ?: return
        if (newPin != confirmPin) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("New passwords do not match."))
            }
            return
        }
        viewModelScope.launch {
            repository.changePassword(driver.driverId, currentPin, newPin).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("6-digit password changed.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Password change failed.")) }
            )
        }
    }

    private fun aggregateRides(rides: List<RideEntity>): PeriodSummary {
        if (rides.isEmpty()) return PeriodSummary()
        return PeriodSummary(
            grossIncome = RideCalculator.round2(rides.sumOf { it.grossFare }),
            netIncome = RideCalculator.round2(rides.sumOf { it.netIncome }),
            totalFuel = RideCalculator.round2(rides.sumOf { it.fuelCost }),
            totalSalik = RideCalculator.round2(rides.sumOf { it.salikCost }),
            totalCommission = RideCalculator.round2(rides.sumOf { it.boltCommission }),
            totalBookingFee = RideCalculator.round2(rides.sumOf { it.bookingFee }),
            totalDistanceKm = RideCalculator.round2(rides.sumOf { it.distanceKm }),
            rideCount = rides.size
        )
    }

    private fun sanitizeDecimalInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val firstDot = filtered.indexOf('.')
        if (firstDot == -1) return filtered.take(8)
        val beforeDot = filtered.substring(0, firstDot).take(6)
        val afterDot = filtered.substring(firstDot + 1).replace(".", "").take(2)
        return "$beforeDot.$afterDot"
    }

    class Factory(private val repository: RideLedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
