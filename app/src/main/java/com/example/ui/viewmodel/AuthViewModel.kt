package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.repository.RideLedgerRepository
import com.example.domain.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(
        val driver: DriverAccountEntity,
        val needsProfileSetup: Boolean
    ) : AuthSessionState
}

enum class AuthMode {
    LOGIN, REGISTER
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val driverIdInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showForgotPasswordDialog: Boolean = false,
    val resetDriverIdInput: String = "",
    val resetMobileInput: String = "",
    val resetNewPasswordInput: String = "",
    val resetConfirmPasswordInput: String = "",
    val resetError: String? = null,
    val isResetting: Boolean = false,
    val profileFullNameInput: String = "",
    val profileMobileInput: String = "",
    val profileError: String? = null,
    val isSavingProfile: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthViewModel(
    private val repository: RideLedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    val sessionState: StateFlow<AuthSessionState> = repository.activeDriverId
        .flatMapLatest { driverId ->
            if (driverId.isNullOrBlank()) {
                flowOf(AuthSessionState.Unauthenticated)
            } else {
                repository.observeDriver(driverId).combine(flowOf(driverId)) { driver, _ ->
                    if (driver == null) {
                        AuthSessionState.Unauthenticated
                    } else {
                        val needsSetup = driver.fullName.isBlank() || driver.mobileNumber.isBlank()
                        AuthSessionState.Authenticated(
                            driver = driver,
                            needsProfileSetup = needsSetup
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthSessionState.Loading
        )

    val biometricDriver: StateFlow<DriverAccountEntity?> = repository.biometricDriverId
        .flatMapLatest { driverId ->
            if (driverId.isNullOrBlank()) flowOf(null) else repository.observeDriver(driverId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                errorMessage = null,
                successMessage = null,
                passwordInput = "",
                confirmPasswordInput = ""
            )
        }
    }

    fun onDriverIdChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(7)
        _uiState.update { it.copy(driverIdInput = digits, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(passwordInput = digits, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(confirmPasswordInput = digits, errorMessage = null) }
    }

    fun submitAuth() {
        val state = _uiState.value
        if (state.isLoading) return

        if (!SecurityUtils.isValidDriverId(state.driverIdInput)) {
            _uiState.update { it.copy(errorMessage = "Driver ID must be exactly 7 digits.") }
            return
        }
        if (!SecurityUtils.isValidPassword(state.passwordInput)) {
            _uiState.update { it.copy(errorMessage = "Password must be exactly 6 digits.") }
            return
        }
        if (state.mode == AuthMode.REGISTER && state.passwordInput != state.confirmPasswordInput) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = if (state.mode == AuthMode.LOGIN) {
                repository.loginDriver(state.driverIdInput, state.passwordInput)
            } else {
                repository.registerDriver(state.driverIdInput, state.passwordInput)
            }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordInput = "",
                            confirmPasswordInput = "",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Authentication failed."
                        )
                    }
                }
            )
        }
    }

    fun openForgotPassword() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                resetDriverIdInput = it.driverIdInput,
                resetMobileInput = "",
                resetNewPasswordInput = "",
                resetConfirmPasswordInput = "",
                resetError = null
            )
        }
    }

    fun dismissForgotPassword() {
        _uiState.update { it.copy(showForgotPasswordDialog = false, resetError = null) }
    }

    fun onResetDriverIdChanged(value: String) {
        _uiState.update {
            it.copy(resetDriverIdInput = value.filter { ch -> ch.isDigit() }.take(7), resetError = null)
        }
    }

    fun onResetMobileChanged(value: String) {
        _uiState.update { it.copy(resetMobileInput = value.take(20), resetError = null) }
    }

    fun onResetNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(resetNewPasswordInput = value.filter { ch -> ch.isDigit() }.take(6), resetError = null)
        }
    }

    fun onResetConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                resetConfirmPasswordInput = value.filter { ch -> ch.isDigit() }.take(6),
                resetError = null
            )
        }
    }

    fun submitForgotPassword() {
        val state = _uiState.value
        if (state.isResetting) return

        if (!SecurityUtils.isValidDriverId(state.resetDriverIdInput)) {
            _uiState.update { it.copy(resetError = "Enter your 7-digit Driver ID.") }
            return
        }
        if (!SecurityUtils.isValidMobile(state.resetMobileInput)) {
            _uiState.update { it.copy(resetError = "Enter your registered mobile number.") }
            return
        }
        if (!SecurityUtils.isValidPassword(state.resetNewPasswordInput)) {
            _uiState.update { it.copy(resetError = "New password must be exactly 6 digits.") }
            return
        }
        if (state.resetNewPasswordInput != state.resetConfirmPasswordInput) {
            _uiState.update { it.copy(resetError = "New passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true, resetError = null) }
            val result = repository.resetPassword(
                driverId = state.resetDriverIdInput,
                mobileNumber = state.resetMobileInput,
                newPassword = state.resetNewPasswordInput
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isResetting = false,
                            showForgotPasswordDialog = false,
                            driverIdInput = state.resetDriverIdInput,
                            passwordInput = "",
                            mode = AuthMode.LOGIN,
                            successMessage = "Password reset locally. Sign in with your new 6-digit password."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isResetting = false,
                            resetError = error.message ?: "Unable to reset password."
                        )
                    }
                }
            )
        }
    }

    fun onProfileFullNameChanged(value: String) {
        _uiState.update { it.copy(profileFullNameInput = value, profileError = null) }
    }

    fun onProfileMobileChanged(value: String) {
        _uiState.update { it.copy(profileMobileInput = value, profileError = null) }
    }

    fun saveInitialProfile(driverId: String) {
        val state = _uiState.value
        if (state.isSavingProfile) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, profileError = null) }
            val result = repository.updateProfile(
                driverId = driverId,
                fullName = state.profileFullNameInput,
                mobileNumber = state.profileMobileInput
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            profileFullNameInput = "",
                            profileMobileInput = "",
                            profileError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            profileError = error.message ?: "Could not save profile."
                        )
                    }
                }
            )
        }
    }

    fun loginWithBiometric() {
        viewModelScope.launch {
            val driverId = biometricDriver.value?.driverId ?: repository.biometricDriverId.first()
            if (driverId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Fingerprint login is not enabled yet.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.loginWithBiometric(driverId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordInput = "",
                            confirmPasswordInput = "",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Fingerprint sign-in failed."
                        )
                    }
                }
            )
        }
    }

    fun reportBiometricError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState()
        }
    }

    class Factory(private val repository: RideLedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
