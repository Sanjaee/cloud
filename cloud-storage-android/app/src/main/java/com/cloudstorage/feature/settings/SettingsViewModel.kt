package com.cloudstorage.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.Quota
import com.cloudstorage.domain.model.SessionItem
import com.cloudstorage.domain.model.User
import com.cloudstorage.domain.repository.AuthRepository
import com.cloudstorage.domain.repository.MfaRepository
import com.cloudstorage.domain.repository.QuotaRepository
import com.cloudstorage.domain.repository.SecurityEvent
import com.cloudstorage.domain.repository.SecurityRepository
import com.cloudstorage.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val quota: Quota? = null,
    val isMfaEnabled: Boolean = false,
    val sessions: List<SessionItem> = emptyList(),
    val securityEvents: List<SecurityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    // MFA setup state
    val mfaSecret: String? = null,
    val mfaQrDataUrl: String? = null,
    val showMfaSetupDialog: Boolean = false,
    val showMfaDisableDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mfaRepository: MfaRepository,
    private val sessionRepository: SessionRepository,
    private val securityRepository: SecurityRepository,
    private val quotaRepository: QuotaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Fetch user (or fallback to stored local user)
            val userRes = authRepository.getCurrentUser()
            val user = if (userRes is NetworkResult.Success) userRes.data else authRepository.getStoredUser()

            // 2. Fetch quota
            val quotaRes = quotaRepository.getQuota()
            val quota = if (quotaRes is NetworkResult.Success) quotaRes.data else null

            // 3. Fetch MFA status
            val mfaRes = mfaRepository.getMfaStatus()
            val isMfa = if (mfaRes is NetworkResult.Success) mfaRes.data else false

            // 4. Fetch Sessions
            val sessRes = sessionRepository.getSessions()
            val sessions = if (sessRes is NetworkResult.Success) sessRes.data else emptyList()

            // 5. Fetch Security Events
            val secRes = securityRepository.getSecurityEvents()
            val events = if (secRes is NetworkResult.Success) secRes.data else emptyList()

            _uiState.update { state ->
                state.copy(
                    user = user,
                    quota = quota,
                    isMfaEnabled = isMfa,
                    sessions = sessions,
                    securityEvents = events,
                    isLoading = false
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    // Sessions management
    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val res = sessionRepository.revokeSession(sessionId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(successMessage = "Sesi berhasil dicabut") }
                    loadSettings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun revokeOtherSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val res = sessionRepository.revokeOtherSessions()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(successMessage = "Semua sesi lain berhasil dicabut!") }
                    loadSettings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // Password management
    fun changePassword(currPass: String, newPass: String, confirmPass: String) {
        if (currPass.isBlank() || newPass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Mohon isi semua field password") }
            return
        }
        if (newPass.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password baru minimal 8 karakter") }
            return
        }
        if (newPass != confirmPass) {
            _uiState.update { it.copy(errorMessage = "Konfirmasi password baru tidak cocok") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val res = securityRepository.changePassword(currPass, newPass)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Password berhasil diperbarui! Silakan login kembali."
                        )
                    }
                    logout()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // 2FA Management
    fun openDisableMfaDialog() {
        _uiState.update { it.copy(showMfaDisableDialog = true) }
    }

    fun closeDisableMfaDialog() {
        _uiState.update { it.copy(showMfaDisableDialog = false) }
    }

    fun disableMfa(codeOrPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val isDigits = codeOrPassword.trim().all { it.isDigit() }
            val res = if (isDigits) {
                mfaRepository.disableMfa(code = codeOrPassword.trim(), password = null)
            } else {
                mfaRepository.disableMfa(code = null, password = codeOrPassword.trim())
            }

            when (res) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            showMfaDisableDialog = false,
                            successMessage = "2FA berhasil dinonaktifkan"
                        )
                    }
                    loadSettings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun openMfaSetupDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val res = mfaRepository.setupMfa()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            mfaSecret = res.data.first,
                            mfaQrDataUrl = res.data.second,
                            showMfaSetupDialog = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun closeMfaSetupDialog() {
        _uiState.update { it.copy(showMfaSetupDialog = false, mfaSecret = null, mfaQrDataUrl = null) }
    }

    fun confirmEnableMfa(totpCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val res = mfaRepository.enableMfa(totpCode)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            showMfaSetupDialog = false,
                            successMessage = "Two-Factor Authentication berhasil diaktifkan!"
                        )
                    }
                    loadSettings()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // Logout
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
