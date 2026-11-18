package com.example

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.StorageSetupDialog
import com.example.ui.screens.AddRideScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteProfileDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RideLedgerTheme
import com.example.ui.viewmodel.AuthSessionState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.util.BiometricAuthHelper
import com.example.util.LocalStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_ADD_RIDE = "add_ride"
    const val ROUTE_REPORTS = "reports"
    const val ROUTE_FINANCE = "finance"
    const val ROUTE_SETTINGS = "settings"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val mainTabs = listOf(
    NavTab(
        route = AppRoutes.ROUTE_DASHBOARD,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    NavTab(
        route = AppRoutes.ROUTE_ADD_RIDE,
        label = "New Ride",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        testTag = "nav_add_ride"
    ),
    NavTab(
        route = AppRoutes.ROUTE_REPORTS,
        label = "Shift",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_reports"
    ),
    NavTab(
        route = AppRoutes.ROUTE_FINANCE,
        label = "Finance",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_finance"
    ),
    NavTab(
        route = AppRoutes.ROUTE_SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as RideLedgerApp).repository

        setContent {
            RideLedgerTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(repository)
                )
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(repository)
                )

                val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val biometricDriver by authViewModel.biometricDriver.collectAsStateWithLifecycle()
                val isBiometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(this@MainActivity) }

                when (val session = sessionState) {
                    AuthSessionState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    AuthSessionState.Unauthenticated -> {
                        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
                            AuthScreen(
                                uiState = authUiState,
                                biometricDriver = biometricDriver,
                                isBiometricAvailable = isBiometricAvailable,
                                onBiometricLogin = {
                                    val enrolled = biometricDriver
                                    val subtitle = if (enrolled != null) {
                                        "Sign in as ${enrolled.fullName.ifBlank { "Driver #${enrolled.driverId}" }}"
                                    } else {
                                        "Verify your fingerprint to sign in"
                                    }
                                    BiometricAuthHelper.authenticate(
                                        activity = this@MainActivity,
                                        title = "RideLedger Fingerprint Sign In",
                                        subtitle = subtitle,
                                        negativeButtonText = "Use 6-Digit Password",
                                        onSuccess = { authViewModel.loginWithBiometric() },
                                        onError = { msg -> authViewModel.reportBiometricError(msg) }
                                    )
                                },
                                onModeChange = authViewModel::setMode,
                                onDriverIdChange = authViewModel::onDriverIdChanged,
                                onPasswordChange = authViewModel::onPasswordChanged,
                                onConfirmPasswordChange = authViewModel::onConfirmPasswordChanged,
                                onSubmitAuth = authViewModel::submitAuth,
                                onOpenForgotPassword = authViewModel::openForgotPassword,
                                onDismissForgotPassword = authViewModel::dismissForgotPassword,
                                onResetDriverIdChange = authViewModel::onResetDriverIdChanged,
                                onResetMobileChange = authViewModel::onResetMobileChanged,
                                onResetNewPasswordChange = authViewModel::onResetNewPasswordChanged,
                                onResetConfirmPasswordChange = authViewModel::onResetConfirmPasswordChanged,
                                onSubmitResetPassword = authViewModel::submitForgotPassword,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    is AuthSessionState.Authenticated -> {
                        AuthenticatedRideLedgerApp(
                            activity = this@MainActivity,
                            mainViewModel = mainViewModel,
                            isBiometricAvailable = isBiometricAvailable,
                            needsProfileSetup = session.needsProfileSetup,
                            onLogout = authViewModel::logout
                        )

                        if (session.needsProfileSetup) {
                            CompleteProfileDialog(
                                driverId = session.driver.driverId,
                                fullName = authUiState.profileFullNameInput,
                                mobileNumber = authUiState.profileMobileInput,
                                error = authUiState.profileError,
                                isSaving = authUiState.isSavingProfile,
                                onFullNameChange = authViewModel::onProfileFullNameChanged,
                                onMobileChange = authViewModel::onProfileMobileChanged,
                                onSave = { authViewModel.saveInitialProfile(session.driver.driverId) },
                                onLogout = authViewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedRideLedgerApp(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    isBiometricAvailable: Boolean,
    needsProfileSetup: Boolean,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val driver by mainViewModel.activeDriver.collectAsStateWithLifecycle()
    val appSettings by mainViewModel.appSettings.collectAsStateWithLifecycle()
    val platforms by mainViewModel.platforms.collectAsStateWithLifecycle()
    val pickupPoints by mainViewModel.pickupPoints.collectAsStateWithLifecycle()
    val allRides by mainViewModel.allRides.collectAsStateWithLifecycle()
    val dashboardState by mainViewModel.dashboardState.collectAsStateWithLifecycle()
    val financeState by mainViewModel.financeState.collectAsStateWithLifecycle()
    val rideFormState by mainViewModel.rideFormState.collectAsStateWithLifecycle()
    val selectedCycle by mainViewModel.selectedReportCycle.collectAsStateWithLifecycle()
    val selectedDateFilter by mainViewModel.selectedReportDateFilter.collectAsStateWithLifecycle()
    val shiftSearchQuery by mainViewModel.shiftSearchQuery.collectAsStateWithLifecycle()
    val selectedPlatformFilter by mainViewModel.selectedPlatformFilter.collectAsStateWithLifecycle()
    val selectedPaymentFilter by mainViewModel.selectedPaymentFilter.collectAsStateWithLifecycle()
    val cycleSummary by mainViewModel.selectedCycleSummary.collectAsStateWithLifecycle()
    val reportGroups by mainViewModel.reportGroups.collectAsStateWithLifecycle()
    val settingsCycle by mainViewModel.settingsSelectedCycle.collectAsStateWithLifecycle()
    val settingsCycleGroups by mainViewModel.settingsCycleGroups.collectAsStateWithLifecycle()
    val settingsCycleSummary by mainViewModel.settingsCycleSummary.collectAsStateWithLifecycle()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val storageSetupCompleted by mainViewModel.storageSetupCompleted.collectAsStateWithLifecycle()
    val grantedFolderUri by mainViewModel.grantedFolderUri.collectAsStateWithLifecycle()

    val storageSetupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            LocalStorageManager.persistTreePermission(activity, treeUri).fold(
                onSuccess = { uriString ->
                    mainViewModel.saveGrantedFolderUri(uriString)
                    val folderName = LocalStorageManager.getTreeFolderDisplayName(activity, uriString)
                    mainViewModel.showUiMessage("Folder access granted ($folderName) & App 'data' folder created.")
                },
                onFailure = { err ->
                    mainViewModel.markStorageSetupCompleted()
                    mainViewModel.showUiMessage(err.message ?: "App 'data' folder ready for local storage.")
                }
            )
        } else {
            mainViewModel.markStorageSetupCompleted()
            mainViewModel.showUiMessage("App 'data' folder created for local storage.")
        }
    }

    LaunchedEffect(driver?.driverId, allRides, appSettings) {
        val active = driver ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            LocalStorageManager.ensureAppDataFolder(activity)
            LocalStorageManager.syncDriverDataBackup(
                context = activity,
                driver = active,
                settings = appSettings,
                rides = allRides
            )
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                UiEvent.RideSavedNavigateBack -> {
                    navController.navigate(AppRoutes.ROUTE_REPORTS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        mainTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        mainTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppRoutes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            driver = driver,
                            dashboardState = dashboardState,
                            financeState = financeState,
                            currencyCode = appSettings.currencyCode,
                            fuelRatePerKm = appSettings.fuelCostPerKm,
                            onAddRideClick = {
                                mainViewModel.resetRideForm()
                                navigateToTab(AppRoutes.ROUTE_ADD_RIDE)
                            },
                            onViewReportsClick = { navigateToTab(AppRoutes.ROUTE_REPORTS) },
                            onOpenFinanceClick = { navigateToTab(AppRoutes.ROUTE_FINANCE) },
                            onLogoutClick = onLogout
                        )
                    }

                    composable(AppRoutes.ROUTE_ADD_RIDE) {
                        AddRideScreen(
                            formState = rideFormState,
                            platforms = platforms,
                            pickupPoints = pickupPoints,
                            appSettings = appSettings,
                            onDateSelected = mainViewModel::onRideDateSelected,
                            onTimeSelected = mainViewModel::onRideTimeSelected,
                            onPlatformSelected = mainViewModel::onPlatformSelected,
                            onPickupPointSelected = mainViewModel::onPickupPointSelected,
                            onPaymentMethodSelected = mainViewModel::onPaymentMethodSelected,
                            onDistanceChanged = mainViewModel::onDistanceChanged,
                            onGrossFareChanged = mainViewModel::onGrossFareChanged,
                            onSalikCostChanged = mainViewModel::onSalikCostChanged,
                            onBoltCommissionChanged = mainViewModel::onBoltCommissionChanged,
                            onBookingFeeChanged = mainViewModel::onBookingFeeChanged,
                            onRememberBookingFeeChanged = mainViewModel::onRememberBookingFeeChanged,
                            onNotesChanged = mainViewModel::onNotesChanged,
                            onSubmitRide = mainViewModel::submitRide,
                            onCancelEdit = {
                                mainViewModel.resetRideForm()
                                navigateToTab(AppRoutes.ROUTE_REPORTS)
                            }
                        )
                    }

                    composable(AppRoutes.ROUTE_REPORTS) {
                        ReportsScreen(
                            driver = driver,
                            appSettings = appSettings,
                            platforms = platforms,
                            selectedCycle = selectedCycle,
                            selectedDateFilter = selectedDateFilter,
                            searchQuery = shiftSearchQuery,
                            selectedPlatformFilter = selectedPlatformFilter,
                            selectedPaymentFilter = selectedPaymentFilter,
                            cycleSummary = cycleSummary,
                            reportGroups = reportGroups,
                            onShiftCycle = mainViewModel::shiftReportCycle,
                            onDateFilterSelected = mainViewModel::setReportDateFilter,
                            onSearchQueryChanged = mainViewModel::onShiftSearchQueryChanged,
                            onPlatformFilterSelected = mainViewModel::onPlatformFilterSelected,
                            onPaymentFilterSelected = mainViewModel::onPaymentFilterSelected,
                            onClearFilters = mainViewModel::clearShiftFilters,
                            onSubmitShiftToFinance = mainViewModel::submitShiftToFinance,
                            onEditRide = { ride ->
                                mainViewModel.startEditingRide(ride)
                                navigateToTab(AppRoutes.ROUTE_ADD_RIDE)
                            },
                            onDeleteRide = mainViewModel::deleteRide,
                            grantedFolderUri = grantedFolderUri,
                            onFolderPermissionGranted = { uriString ->
                                mainViewModel.saveGrantedFolderUri(uriString)
                            },
                            onExportCompleted = mainViewModel::notifyExportResult,
                            onImportExcel = mainViewModel::importRidesFromExcel,
                            onUiMessage = mainViewModel::showUiMessage
                        )
                    }

                    composable(AppRoutes.ROUTE_FINANCE) {
                        FinanceScreen(
                            financeState = financeState,
                            currencyCode = appSettings.currencyCode,
                            onSubmitShiftToFinance = mainViewModel::submitShiftToFinance,
                            onSubmitAllPending = mainViewModel::submitAllPendingToFinance
                        )
                    }

                    composable(AppRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            driver = driver,
                            appSettings = appSettings,
                            platforms = platforms,
                            pickupPoints = pickupPoints,
                            isBiometricAvailable = isBiometricAvailable,
                            isBiometricEnabled = isBiometricEnabled,
                            onToggleBiometric = { enable ->
                                if (enable) {
                                    BiometricAuthHelper.authenticate(
                                        activity = activity,
                                        title = "Enable Fingerprint Login",
                                        subtitle = "Verify your fingerprint for Driver #${driver?.driverId ?: ""}",
                                        negativeButtonText = "Cancel",
                                        onSuccess = { mainViewModel.setBiometricEnabled(true) },
                                        onError = { msg -> mainViewModel.showUiMessage(msg) }
                                    )
                                } else {
                                    mainViewModel.setBiometricEnabled(false)
                                }
                            },
                            onUpdateProfile = mainViewModel::updateProfile,
                            onChangePassword = mainViewModel::changePassword,
                            onUpdateFuelAndCurrency = mainViewModel::updateFuelAndCurrency,
                            onUpdateShiftTiming = mainViewModel::updateShiftTiming,
                            onAddPlatform = mainViewModel::addPlatform,
                            onDeletePlatform = mainViewModel::deletePlatform,
                            onAddPickupPoint = mainViewModel::addPickupPoint,
                            onUpdatePickupPoint = mainViewModel::updatePickupPoint,
                            onDeletePickupPoint = mainViewModel::deletePickupPoint,
                            settingsCycle = settingsCycle,
                            settingsCycleSummary = settingsCycleSummary,
                            settingsCycleGroups = settingsCycleGroups,
                            onShiftSettingsCycle = mainViewModel::shiftSettingsCycle,
                            onDeleteCycleData = mainViewModel::deleteCycleData,
                            grantedFolderUri = grantedFolderUri,
                            onFolderPermissionGranted = { uriString ->
                                mainViewModel.saveGrantedFolderUri(uriString)
                            },
                            onImportExcel = mainViewModel::importRidesFromExcel,
                            onExportCompleted = mainViewModel::notifyExportResult,
                            onUiMessage = mainViewModel::showUiMessage,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }

    if (!needsProfileSetup && driver != null && !storageSetupCompleted) {
        val appDataPath = remember { LocalStorageManager.getAppDataFolderDisplayPath(activity) }
        StorageSetupDialog(
            appDataFolderPath = appDataPath,
            onGrantFolderPermission = {
                runCatching { storageSetupFolderLauncher.launch(null) }
                    .onFailure {
                        mainViewModel.markStorageSetupCompleted()
                        mainViewModel.showUiMessage("Local app 'data' folder created at $appDataPath")
                    }
            },
            onUseAppDataFolder = {
                LocalStorageManager.ensureAppDataFolder(activity)
                mainViewModel.markStorageSetupCompleted()
                mainViewModel.showUiMessage("Local 'data' folder ready inside app storage.")
            }
        )
    }
}
