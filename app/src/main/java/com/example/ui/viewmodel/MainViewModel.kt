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
import com.example.domain.BillingCycle
import com.example.domain.DailySummaryItem
import com.example.domain.FinanceSummaryCalculator
import com.example.domain.FinanceUiState
import com.example.domain.RideCalculator
import com.example.util.InAppReminderPrompt
import com.example.util.RideReminderScheduler
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
    val rideCount: Int = 0,
    val totalTips: Double = 0.0
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
    val todayShiftIso: String = RideCalculator.currentIsoDate(),
    val currentBillingCycle: BillingCycle = RideCalculator.getBillingCycleForDate(RideCalculator.currentIsoDate()),
    val shiftWindowLabel: String = "00:00 – 23:59",
    val todaySummary: PeriodSummary = PeriodSummary(),
    val monthSummary: PeriodSummary = PeriodSummary(),
    val todayRides: List<RideEntity> = emptyList(),
    val monthlyChartPoints: List<DailyChartPoint> = emptyList(),
    val platformBreakdown: List<PlatformSummary> = emptyList()
)

data class DailyReportGroup(
    val dateIso: String,
    val summary: PeriodSummary,
    val rides: List<RideEntity>,
    val unsubmittedShiftRideIds: List<Long> = emptyList(),
    val unsubmittedShiftGross: Double = 0.0,
    val totalShiftRideCount: Int = rides.size
) {
    val isShiftSubmittedToFinance: Boolean
        get() = totalShiftRideCount > 0 && unsubmittedShiftRideIds.isEmpty()
}

data class RideFormState(
    val editingRideId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val dateIso: String = RideCalculator.currentIsoDate(),
    val rideHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val rideMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val selectedPlatform: String = "",
    val selectedPickupPoint: String = "",
    val paymentMethod: String = "Cash",
    val distanceInput: String = "",
    val grossFareInput: String = "",
    val tipsInput: String = "",
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
    val rawTips: Double get() = tipsInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val salikCost: Double get() = salikCostInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rawCommission: Double get() = boltCommissionInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rawBookingFee: Double get() = bookingFeeInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val showPickupPoint: Boolean
        get() = RideCalculator.showsPickupPoint(selectedPlatform)

    val isDtcEventBooking: Boolean
        get() = RideCalculator.isDtcEventBooking(selectedPlatform, selectedPickupPoint)

    val showDistanceAndFuel: Boolean
        get() = RideCalculator.showsDistanceAndFuel(selectedPlatform, selectedPickupPoint)

    val showSalikCost: Boolean
        get() = RideCalculator.showsSalikCost(selectedPlatform, selectedPickupPoint)

    val showTips: Boolean
        get() = RideCalculator.showsTips(selectedPlatform, selectedPickupPoint)

    val showBoltCommission: Boolean
        get() = showPickupPoint && RideCalculator.showsBoltCommission(selectedPlatform, selectedPickupPoint)

    val showBookingFee: Boolean
        get() = RideCalculator.showsBookingFee(selectedPlatform, selectedPickupPoint)

    val allowedPaymentMethods: List<String>
        get() = RideCalculator.allowedPaymentMethods(selectedPlatform)

    val usesYourEarningLabel: Boolean
        get() = RideCalculator.usesYourEarningLabel(selectedPlatform)

    val effectiveDistanceKm: Double
        get() = if (showDistanceAndFuel) distanceKm else 0.0

    val effectiveTips: Double
        get() = if (showTips) rawTips else 0.0

    val effectiveSalikCost: Double
        get() = if (showSalikCost) salikCost else 0.0

    val effectiveCommission: Double
        get() = if (showBoltCommission) rawCommission else 0.0

    val effectiveBookingFee: Double
        get() = if (showBookingFee) rawBookingFee else 0.0

    val combinedTimestampMillis: Long
        get() = RideCalculator.combineDateAndTime(dateMillis, rideHour, rideMinute)

    fun computedShiftDateIso(settings: AppSettingsEntity): String =
        RideCalculator.computeShiftDateIso(combinedTimestampMillis, settings)

    fun isWithinShift(settings: AppSettingsEntity): Boolean =
        RideCalculator.isTimeWithinShift(rideHour, rideMinute, settings)

    fun computedFuelCost(fuelCostPerKm: Double): Double =
        if (showDistanceAndFuel) RideCalculator.calculateFuelCost(distanceKm, fuelCostPerKm) else 0.0

    fun computedNetIncome(fuelCostPerKm: Double): Double =
        if (isDtcEventBooking) {
            RideCalculator.round2(grossFare)
        } else {
            RideCalculator.calculateNetIncome(
                grossFare = grossFare,
                fuelCost = computedFuelCost(fuelCostPerKm),
                salikCost = effectiveSalikCost,
                boltCommission = effectiveCommission,
                bookingFee = effectiveBookingFee,
                tips = effectiveTips
            )
        }
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

    val remindersEnabled: StateFlow<Boolean> = repository.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _reminderClockTick = MutableStateFlow(System.currentTimeMillis())

    val activeInAppReminder: StateFlow<InAppReminderPrompt?> = combine(
        remindersEnabled,
        repository.dismissedReminderSlot,
        _reminderClockTick
    ) { enabled, dismissedSlot, nowMillis ->
        if (!enabled) {
            null
        } else {
            val active = RideReminderScheduler.activeInAppReminder(nowMillis)
            if (active != null && active.slotKey != dismissedSlot) active else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    val dashboardState: StateFlow<DashboardUiState> = combine(allRides, appSettings) { rides, settings ->
        val todayShiftIso = RideCalculator.currentShiftIsoDate(settings)
        val billingCycle = RideCalculator.getBillingCycleForDate(todayShiftIso)

        val ridesWithShift = rides.map { ride ->
            ride to RideCalculator.computeShiftDateIso(ride.rideDateMillis, settings)
        }

        val todayRides = ridesWithShift
            .filter { (_, shiftIso) -> shiftIso == todayShiftIso }
            .map { (ride, _) -> ride }

        val cycleRidesWithShift = ridesWithShift
            .filter { (_, shiftIso) ->
                shiftIso >= billingCycle.startDateIso && shiftIso <= billingCycle.endDateIso
            }

        val monthRides = cycleRidesWithShift.map { (ride, _) -> ride }

        val chartPoints = cycleRidesWithShift
            .groupBy { (_, shiftIso) -> shiftIso }
            .entries
            .sortedBy { it.key }
            .map { (shiftIso, entries) ->
                val dayNum = shiftIso.takeLast(2).toIntOrNull() ?: 1
                DailyChartPoint(
                    dayOfMonth = dayNum,
                    dateIso = shiftIso,
                    grossIncome = RideCalculator.round2(entries.sumOf { it.first.grossFare }),
                    netIncome = RideCalculator.round2(entries.sumOf { it.first.netIncome })
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
            todayShiftIso = todayShiftIso,
            currentBillingCycle = billingCycle,
            shiftWindowLabel = RideCalculator.formatShiftWindow(settings),
            todaySummary = aggregateRides(todayRides),
            monthSummary = aggregateRides(monthRides),
            todayRides = todayRides,
            monthlyChartPoints = chartPoints,
            platformBreakdown = platformBreakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    val isBiometricEnabled: StateFlow<Boolean> = combine(
        repository.biometricDriverId,
        repository.activeDriverId
    ) { biometricId, activeId ->
        !activeId.isNullOrBlank() && biometricId == activeId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val storageSetupCompleted: StateFlow<Boolean> = repository.storageSetupCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val grantedFolderUri: StateFlow<String?> = repository.grantedFolderUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val financeState: StateFlow<FinanceUiState> = combine(allRides, appSettings) { rides, settings ->
        FinanceSummaryCalculator.computeFinanceState(rides, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    val activeFinanceInAppReminder: StateFlow<InAppReminderPrompt?> = combine(
        remindersEnabled,
        financeState,
        appSettings,
        repository.dismissedFinanceReminderSlot,
        _reminderClockTick
    ) { enabled, finance, settings, dismissedSlot, nowMillis ->
        if (!enabled) {
            null
        } else {
            val active = RideReminderScheduler.activeFinanceInAppReminder(
                nowMillis = nowMillis,
                financeState = finance,
                currencyCode = settings.currencyCode
            )
            if (active != null && active.slotKey != dismissedSlot) active else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedReportCycle = MutableStateFlow(
        RideCalculator.getBillingCycleForDate(RideCalculator.currentIsoDate())
    )
    val selectedReportCycle: StateFlow<BillingCycle> = _selectedReportCycle

    private val _dailySummaryCycle = MutableStateFlow(
        RideCalculator.getBillingCycleForDate(RideCalculator.currentIsoDate())
    )
    val dailySummaryCycle: StateFlow<BillingCycle> = _dailySummaryCycle

    val dailySummaryItems: StateFlow<List<DailySummaryItem>> = combine(
        allRides,
        appSettings,
        _dailySummaryCycle
    ) { rides, settings, cycle ->
        FinanceSummaryCalculator.buildDailyCycleBreakdown(rides, settings, cycle)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummaryMonthTotals: StateFlow<PeriodSummary> = combine(
        dailySummaryItems,
        flowOf(Unit)
    ) { items, _ ->
        if (items.isEmpty()) {
            PeriodSummary()
        } else {
            PeriodSummary(
                grossIncome = RideCalculator.round2(items.sumOf { it.summary.grossIncome }),
                netIncome = RideCalculator.round2(items.sumOf { it.summary.netIncome }),
                totalFuel = RideCalculator.round2(items.sumOf { it.summary.totalFuel }),
                totalSalik = RideCalculator.round2(items.sumOf { it.summary.totalSalik }),
                totalCommission = RideCalculator.round2(items.sumOf { it.summary.totalCommission }),
                totalBookingFee = RideCalculator.round2(items.sumOf { it.summary.totalBookingFee }),
                totalDistanceKm = RideCalculator.round2(items.sumOf { it.summary.totalDistanceKm }),
                rideCount = items.sumOf { it.summary.rideCount },
                totalTips = RideCalculator.round2(items.sumOf { it.summary.totalTips })
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    private val _selectedReportDateFilter = MutableStateFlow<String?>(null)
    val selectedReportDateFilter: StateFlow<String?> = _selectedReportDateFilter

    private val _shiftSearchQuery = MutableStateFlow("")
    val shiftSearchQuery: StateFlow<String> = _shiftSearchQuery

    private val _selectedPlatformFilter = MutableStateFlow<String?>(null)
    val selectedPlatformFilter: StateFlow<String?> = _selectedPlatformFilter

    private val _selectedPaymentFilter = MutableStateFlow<String?>(null)
    val selectedPaymentFilter: StateFlow<String?> = _selectedPaymentFilter

    private data class ReportFilterCriteria(
        val cycle: BillingCycle,
        val dateFilter: String?,
        val searchQuery: String,
        val platformFilter: String?,
        val paymentFilter: String?
    )

    private val reportFilterCriteria = combine(
        _selectedReportCycle,
        _selectedReportDateFilter,
        _shiftSearchQuery,
        _selectedPlatformFilter,
        _selectedPaymentFilter
    ) { cycle, dateFilter, query, platform, payment ->
        ReportFilterCriteria(
            cycle = cycle,
            dateFilter = dateFilter,
            searchQuery = query,
            platformFilter = platform,
            paymentFilter = payment
        )
    }

    val reportGroups: StateFlow<List<DailyReportGroup>> = combine(
        allRides,
        appSettings,
        reportFilterCriteria
    ) { rides, settings, criteria ->
        val ridesWithShift = rides.map { ride ->
            ride to RideCalculator.computeShiftDateIso(ride.rideDateMillis, settings)
        }
        val periodFiltered = if (criteria.dateFilter != null) {
            ridesWithShift.filter { (_, shiftIso) -> shiftIso == criteria.dateFilter }
        } else {
            ridesWithShift.filter { (_, shiftIso) ->
                shiftIso >= criteria.cycle.startDateIso && shiftIso <= criteria.cycle.endDateIso
            }
        }

        val shiftStatusMap = periodFiltered.groupBy { (_, shiftIso) -> shiftIso }

        val searchFiltered = periodFiltered.filter { (ride, shiftIso) ->
            FinanceSummaryCalculator.matchesShiftFilter(
                ride = ride,
                shiftDateIso = shiftIso,
                searchQuery = criteria.searchQuery,
                platformFilter = criteria.platformFilter,
                paymentFilter = criteria.paymentFilter
            )
        }

        searchFiltered
            .groupBy { (_, shiftIso) -> shiftIso }
            .entries
            .sortedByDescending { it.key }
            .map { (shiftIso, entries) ->
                val dayRides = entries.map { it.first }.sortedByDescending { it.rideDateMillis }
                val allDayRides = shiftStatusMap[shiftIso]?.map { it.first }.orEmpty()
                val unsubmitted = allDayRides.filter { !it.isSubmittedToFinance }
                DailyReportGroup(
                    dateIso = shiftIso,
                    summary = aggregateRides(dayRides),
                    rides = dayRides,
                    unsubmittedShiftRideIds = unsubmitted.map { it.id },
                    unsubmittedShiftGross = RideCalculator.round2(unsubmitted.sumOf { it.grossFare + it.tips }),
                    totalShiftRideCount = allDayRides.size
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCycleSummary: StateFlow<PeriodSummary> = combine(
        reportGroups,
        flowOf(Unit)
    ) { groups, _ ->
        aggregateRides(groups.flatMap { it.rides })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    private val _settingsSelectedCycle = MutableStateFlow(
        RideCalculator.getBillingCycleForDate(RideCalculator.currentIsoDate())
    )
    val settingsSelectedCycle: StateFlow<BillingCycle> = _settingsSelectedCycle

    val settingsCycleGroups: StateFlow<List<DailyReportGroup>> = combine(
        allRides,
        appSettings,
        _settingsSelectedCycle
    ) { rides, settings, cycle ->
        rides
            .map { ride -> ride to RideCalculator.computeShiftDateIso(ride.rideDateMillis, settings) }
            .filter { (_, shiftIso) -> shiftIso >= cycle.startDateIso && shiftIso <= cycle.endDateIso }
            .groupBy { (_, shiftIso) -> shiftIso }
            .entries
            .sortedByDescending { it.key }
            .map { (shiftIso, entries) ->
                val dayRides = entries.map { it.first }.sortedByDescending { it.rideDateMillis }
                DailyReportGroup(
                    dateIso = shiftIso,
                    summary = aggregateRides(dayRides),
                    rides = dayRides
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsCycleSummary: StateFlow<PeriodSummary> = combine(
        settingsCycleGroups,
        flowOf(Unit)
    ) { groups, _ ->
        aggregateRides(groups.flatMap { it.rides })
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
                    val allowedPickups = pickupList.filter {
                        RideCalculator.isPickupPointAllowedForPlatform(updated.selectedPlatform, it.name)
                    }
                    val currentAllowed = allowedPickups.any {
                        it.name.equals(updated.selectedPickupPoint, ignoreCase = true)
                    }
                    if ((updated.selectedPickupPoint.isBlank() || !currentAllowed) && allowedPickups.isNotEmpty()) {
                        updated = updated.copy(selectedPickupPoint = allowedPickups.first().name)
                        updated = applyDefaultBookingFeeIfEmpty(updated, pickupList)
                    }
                    val allowedPayments = RideCalculator.allowedPaymentMethods(updated.selectedPlatform)
                    if (allowedPayments.none { it.equals(updated.paymentMethod, ignoreCase = true) }) {
                        val fallbackPayment = if (allowedPayments.any { it.equals("Cash", ignoreCase = true) }) {
                            "Cash"
                        } else {
                            allowedPayments.firstOrNull() ?: "Cash"
                        }
                        updated = updated.copy(paymentMethod = fallbackPayment)
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
        if (!RideCalculator.showsBookingFee(state.selectedPlatform, state.selectedPickupPoint)) {
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

    fun onRideTimeSelected(hour: Int, minute: Int) {
        _rideFormState.update {
            it.copy(
                rideHour = hour.coerceIn(0, 23),
                rideMinute = minute.coerceIn(0, 59),
                errorMessage = null
            )
        }
    }

    fun onPlatformSelected(platformName: String) {
        val pickupList = pickupPoints.value
        _rideFormState.update { current ->
            val allowedPickups = pickupList.filter {
                RideCalculator.isPickupPointAllowedForPlatform(platformName, it.name)
            }
            val currentAllowed = allowedPickups.any {
                it.name.equals(current.selectedPickupPoint, ignoreCase = true)
            }
            val resolvedPickup = when {
                !RideCalculator.showsPickupPoint(platformName) -> current.selectedPickupPoint
                currentAllowed -> current.selectedPickupPoint
                else -> allowedPickups.firstOrNull()?.name ?: ""
            }
            val allowedPayments = RideCalculator.allowedPaymentMethods(platformName)
            val resolvedPayment = when {
                allowedPayments.any { it.equals(current.paymentMethod, ignoreCase = true) } -> current.paymentMethod
                allowedPayments.any { it.equals("Cash", ignoreCase = true) } -> "Cash"
                else -> allowedPayments.firstOrNull() ?: "Cash"
            }
            val withPlatform = current.copy(
                selectedPlatform = platformName,
                selectedPickupPoint = resolvedPickup,
                paymentMethod = resolvedPayment,
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

    fun onPaymentMethodSelected(method: String) {
        _rideFormState.update { it.copy(paymentMethod = method, errorMessage = null) }
    }

    fun onDistanceChanged(value: String) {
        _rideFormState.update { it.copy(distanceInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onGrossFareChanged(value: String) {
        _rideFormState.update { it.copy(grossFareInput = sanitizeDecimalInput(value), errorMessage = null) }
    }

    fun onTipsChanged(value: String) {
        _rideFormState.update { it.copy(tipsInput = sanitizeDecimalInput(value), errorMessage = null) }
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
        val cal = Calendar.getInstance().apply { timeInMillis = ride.rideDateMillis }
        val fallbackPickup = pickupPoints.value.firstOrNull()?.name ?: ""
        _rideFormState.value = RideFormState(
            editingRideId = ride.id,
            dateMillis = ride.rideDateMillis,
            dateIso = RideCalculator.toIsoDate(ride.rideDateMillis),
            rideHour = cal.get(Calendar.HOUR_OF_DAY),
            rideMinute = cal.get(Calendar.MINUTE),
            selectedPlatform = ride.platformName,
            selectedPickupPoint = ride.pickupPointName.ifBlank { fallbackPickup },
            paymentMethod = ride.paymentMethod.ifBlank { "Cash" },
            distanceInput = if (ride.distanceKm > 0.0) formatEditableNumber(ride.distanceKm) else "",
            grossFareInput = formatEditableNumber(ride.grossFare),
            tipsInput = if (ride.tips > 0.0) formatEditableNumber(ride.tips) else "",
            salikCostInput = if (ride.salikCost > 0.0) formatEditableNumber(ride.salikCost) else "",
            boltCommissionInput = if (ride.boltCommission > 0.0) formatEditableNumber(ride.boltCommission) else "",
            bookingFeeInput = if (ride.bookingFee > 0.0) formatEditableNumber(ride.bookingFee) else "",
            rememberBookingFee = false,
            notesInput = ride.notes
        )
    }

    fun resetRideForm() {
        val now = Calendar.getInstance()
        val defaultPlatform = platforms.value.firstOrNull()?.name ?: ""
        val allowedPickups = pickupPoints.value.filter {
            RideCalculator.isPickupPointAllowedForPlatform(defaultPlatform, it.name)
        }
        val defaultPickup = (allowedPickups.firstOrNull() ?: pickupPoints.value.firstOrNull())?.name ?: ""
        val allowedPayments = RideCalculator.allowedPaymentMethods(defaultPlatform)
        val defaultPayment = if (allowedPayments.any { it.equals("Cash", ignoreCase = true) }) {
            "Cash"
        } else {
            allowedPayments.firstOrNull() ?: "Cash"
        }
        val initial = RideFormState(
            dateMillis = now.timeInMillis,
            dateIso = RideCalculator.toIsoDate(now.timeInMillis),
            rideHour = now.get(Calendar.HOUR_OF_DAY),
            rideMinute = now.get(Calendar.MINUTE),
            selectedPlatform = defaultPlatform,
            selectedPickupPoint = defaultPickup,
            paymentMethod = defaultPayment
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
        if (state.showPickupPoint && state.selectedPickupPoint.isBlank()) {
            _rideFormState.update { it.copy(errorMessage = "Please add and select a pickup point.") }
            return
        }
        val distanceVal = if (!state.showDistanceAndFuel) {
            0.0
        } else {
            state.distanceInput.toDoubleOrNull()?.takeIf { it >= 0.0 }
                ?: run {
                    _rideFormState.update { it.copy(errorMessage = "Enter a valid distance (km).") }
                    return
                }
        }
        val grossVal = state.grossFareInput.toDoubleOrNull()
        if (grossVal == null || grossVal <= 0.0) {
            val fareLabel = when {
                state.isDtcEventBooking -> "Full Fare"
                state.usesYourEarningLabel -> "Your Earning"
                else -> "Fare"
            }
            _rideFormState.update { it.copy(errorMessage = "Enter a valid $fareLabel greater than 0.") }
            return
        }

        val settings = appSettings.value
        val fuelRate = if (state.showDistanceAndFuel) settings.fuelCostPerKm else 0.0
        val fuelCost = if (state.showDistanceAndFuel) state.computedFuelCost(fuelRate) else 0.0
        val salikCost = if (state.showSalikCost) RideCalculator.round2(state.salikCost) else 0.0
        val boltCommission = RideCalculator.round2(state.effectiveCommission)
        val bookingFee = RideCalculator.round2(state.effectiveBookingFee)
        val tips = RideCalculator.round2(state.effectiveTips)
        val netIncome = if (state.isDtcEventBooking) {
            RideCalculator.round2(grossVal)
        } else {
            RideCalculator.calculateNetIncome(
                grossFare = grossVal,
                fuelCost = fuelCost,
                salikCost = salikCost,
                boltCommission = boltCommission,
                bookingFee = bookingFee,
                tips = tips
            )
        }
        val timestampMillis = state.combinedTimestampMillis
        val shiftDateIso = RideCalculator.computeShiftDateIso(timestampMillis, settings)
        val effectivePickup = if (state.showPickupPoint) state.selectedPickupPoint else ""

        val resolvedPaymentMethod = state.allowedPaymentMethods.firstOrNull {
            it.equals(state.paymentMethod, ignoreCase = true)
        } ?: state.allowedPaymentMethods.firstOrNull() ?: "Cash"

        val wasEditing = state.editingRideId != null

        viewModelScope.launch {
            _rideFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val existingRide = state.editingRideId?.let { repository.getRideById(it) }
            val entity = RideEntity(
                id = state.editingRideId ?: 0L,
                driverId = driver.driverId,
                rideDateMillis = timestampMillis,
                rideDateIso = shiftDateIso,
                platformName = state.selectedPlatform,
                pickupPointName = effectivePickup,
                paymentMethod = resolvedPaymentMethod,
                distanceKm = RideCalculator.round2(distanceVal),
                grossFare = RideCalculator.round2(grossVal),
                fuelCostPerKmApplied = fuelRate,
                fuelCost = fuelCost,
                salikCost = salikCost,
                boltCommission = boltCommission,
                bookingFee = bookingFee,
                netIncome = netIncome,
                tips = tips,
                notes = state.notesInput.trim(),
                isSubmittedToFinance = existingRide?.isSubmittedToFinance ?: false,
                financeSubmittedAt = existingRide?.financeSubmittedAt,
                createdAt = existingRide?.createdAt ?: System.currentTimeMillis()
            )
            repository.saveRide(entity)

            if (state.showBookingFee && state.rememberBookingFee && effectivePickup.isNotBlank()) {
                repository.saveBookingFeeForPickupPoint(
                    pickupPointName = effectivePickup,
                    platformName = state.selectedPlatform,
                    bookingFee = bookingFee
                )
            }

            val refreshedPickups = pickupPoints.first()
            val now = Calendar.getInstance()
            val resetState = RideFormState(
                dateMillis = state.dateMillis,
                dateIso = state.dateIso,
                rideHour = now.get(Calendar.HOUR_OF_DAY),
                rideMinute = now.get(Calendar.MINUTE),
                selectedPlatform = state.selectedPlatform,
                selectedPickupPoint = state.selectedPickupPoint,
                paymentMethod = state.paymentMethod,
                rememberBookingFee = state.rememberBookingFee
            )
            _rideFormState.value = applyDefaultBookingFeeIfEmpty(
                resetState,
                refreshedPickups,
                forceOverwrite = true
            )

            _events.emit(
                UiEvent.ShowMessage(
                    if (wasEditing) "Ride updated for shift $shiftDateIso." else "Ride saved to shift $shiftDateIso."
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

    fun shiftReportCycle(deltaMonths: Int) {
        _selectedReportDateFilter.value = null
        _selectedReportCycle.update { current ->
            RideCalculator.shiftBillingCycle(current.startDateIso, deltaMonths)
        }
    }

    fun shiftDailySummaryCycle(deltaMonths: Int) {
        _dailySummaryCycle.update { current ->
            RideCalculator.shiftBillingCycle(current.startDateIso, deltaMonths)
        }
    }

    fun setReportDateFilter(dateIso: String?) {
        _selectedReportDateFilter.value = dateIso
        if (dateIso != null) {
            _selectedReportCycle.value = RideCalculator.getBillingCycleForDate(dateIso)
        }
    }

    fun onShiftSearchQueryChanged(query: String) {
        _shiftSearchQuery.value = query
    }

    fun onPlatformFilterSelected(platformName: String?) {
        _selectedPlatformFilter.value = platformName
    }

    fun onPaymentFilterSelected(paymentMethod: String?) {
        _selectedPaymentFilter.value = paymentMethod
    }

    fun clearShiftFilters() {
        _shiftSearchQuery.value = ""
        _selectedPlatformFilter.value = null
        _selectedPaymentFilter.value = null
    }

    fun submitShiftToFinance(rideIds: List<Long>, shiftDateIso: String) {
        if (rideIds.isEmpty()) return
        viewModelScope.launch {
            repository.submitRidesToFinance(rideIds)
            _events.emit(UiEvent.ShowMessage("Shift $shiftDateIso submitted to Finance."))
        }
    }

    fun submitAllPendingToFinance() {
        val pendingIds = financeState.value.allPendingRideIds
        if (pendingIds.isEmpty()) return
        viewModelScope.launch {
            repository.submitRidesToFinance(pendingIds)
            _events.emit(UiEvent.ShowMessage("All pending shift income submitted to Finance."))
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val driver = activeDriver.value ?: return
        viewModelScope.launch {
            repository.setBiometricEnabled(driver.driverId, enabled)
            _events.emit(
                UiEvent.ShowMessage(
                    if (enabled) "Fingerprint login enabled for Driver #${driver.driverId}."
                    else "Fingerprint login disabled."
                )
            )
        }
    }

    fun showUiMessage(message: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage(message))
        }
    }

    fun markStorageSetupCompleted() {
        viewModelScope.launch {
            repository.setStorageSetupCompleted(true)
        }
    }

    fun saveGrantedFolderUri(uriString: String?) {
        viewModelScope.launch {
            repository.setGrantedFolderUri(uriString)
            if (!uriString.isNullOrBlank()) {
                repository.setStorageSetupCompleted(true)
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRemindersEnabled(enabled)
            _reminderClockTick.value = System.currentTimeMillis()
            _events.emit(
                UiEvent.ShowMessage(
                    if (enabled) "Daily 4:00 PM & 4:00 AM ride reminders enabled."
                    else "Daily ride reminders paused."
                )
            )
        }
    }

    fun dismissActiveInAppReminder(slotKey: String) {
        viewModelScope.launch {
            repository.setDismissedReminderSlot(slotKey)
        }
    }

    fun dismissFinanceInAppReminder(slotKey: String) {
        viewModelScope.launch {
            repository.setDismissedFinanceReminderSlot(slotKey)
        }
    }

    fun refreshInAppReminder() {
        _reminderClockTick.value = System.currentTimeMillis()
    }

    fun addPlatform(name: String) {
        val clean = RideCalculator.normalizePlatformInput(name)
        viewModelScope.launch {
            repository.addPlatform(clean).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Platform \"$clean\" added.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Failed to add platform.")) }
            )
        }
    }

    fun deletePlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            repository.deletePlatform(platform).fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("Platform \"${platform.name}\" removed.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Default platforms cannot be deleted.")) }
            )
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

    fun updateShiftTiming(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            repository.updateShiftTiming(startHour, startMinute, endHour, endMinute).fold(
                onSuccess = {
                    val label = "${RideCalculator.toTimeLabel(startHour, startMinute)} – ${RideCalculator.toTimeLabel(endHour, endMinute)}"
                    _events.emit(UiEvent.ShowMessage("Shift timing saved: $label"))
                },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "Could not save shift timing.")) }
            )
        }
    }

    fun shiftSettingsCycle(deltaMonths: Int) {
        _settingsSelectedCycle.update { current ->
            RideCalculator.shiftBillingCycle(current.startDateIso, deltaMonths)
        }
    }

    fun deleteCycleData(cycle: BillingCycle) {
        val driver = activeDriver.value ?: return
        val settings = appSettings.value
        viewModelScope.launch {
            val deletedCount = repository.deleteRidesForCycle(driver.driverId, cycle, settings)
            if (deletedCount > 0) {
                _events.emit(UiEvent.ShowMessage("Deleted $deletedCount rides for ${cycle.label}."))
            } else {
                _events.emit(UiEvent.ShowMessage("No rides found to delete in ${cycle.label}."))
            }
        }
    }

    fun importRidesFromExcel(result: Result<List<RideEntity>>) {
        val driver = activeDriver.value ?: return
        viewModelScope.launch {
            result.fold(
                onSuccess = { parsedRides ->
                    if (parsedRides.isEmpty()) {
                        _events.emit(UiEvent.ShowMessage("No valid ride rows found in the selected Excel file."))
                        return@launch
                    }
                    val importResult = repository.importBackupRides(driver.driverId, parsedRides)
                    val msg = when {
                        importResult.importedCount > 0 && importResult.skippedDuplicates > 0 ->
                            "Imported ${importResult.importedCount} rides (${importResult.skippedDuplicates} duplicates skipped)."
                        importResult.importedCount > 0 ->
                            "Imported ${importResult.importedCount} rides from Excel backup."
                        else ->
                            "All ${importResult.skippedDuplicates} rides from this Excel file already exist."
                    }
                    _events.emit(UiEvent.ShowMessage(msg))
                },
                onFailure = { error ->
                    _events.emit(UiEvent.ShowMessage(error.message ?: "Excel import failed."))
                }
            )
        }
    }

    fun notifyExportResult(result: Result<Unit>, formatLabel: String) {
        viewModelScope.launch {
            result.fold(
                onSuccess = { _events.emit(UiEvent.ShowMessage("$formatLabel report saved to device.")) },
                onFailure = { _events.emit(UiEvent.ShowMessage(it.message ?: "$formatLabel export failed.")) }
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

    private fun aggregateRides(rides: List<RideEntity>): PeriodSummary =
        FinanceSummaryCalculator.aggregateRides(rides)

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
