package com.cloudstorage.feature.auth.authenticator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MfaVerificationUiState {
    object Idle : MfaVerificationUiState
    object Loading : MfaVerificationUiState
    object Success : MfaVerificationUiState
    data class Error(val message: String) : MfaVerificationUiState
}

@HiltViewModel
class MfaVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val mfaToken: String = savedStateHandle["mfaToken"] ?: ""
    val email: String = savedStateHandle["email"] ?: ""

    private val _uiState = MutableStateFlow<MfaVerificationUiState>(MfaVerificationUiState.Idle)
    val uiState: StateFlow<MfaVerificationUiState> = _uiState.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    fun onCodeChanged(newCode: String) {
        // If user pastes multi-code text, extract single candidate
        val trimmed = newCode.trim()
        val regex = Regex("[A-Za-z0-9]{4}-[A-Za-z0-9]{4}|[A-Za-z0-9]{8}")
        val match = regex.find(trimmed)
        if (match != null && trimmed.length > 9) {
            _code.value = match.value.uppercase()
        } else {
            _code.value = newCode.uppercase()
        }
    }

    fun verify() {
        val currentCode = _code.value.trim()
        if (currentCode.isEmpty()) {
            _uiState.value = MfaVerificationUiState.Error("Masukkan 6-digit kode TOTP atau Backup Code")
            return
        }

        viewModelScope.launch {
            _uiState.value = MfaVerificationUiState.Loading

            when (val res = authRepository.verifyGoogleMfa(mfaToken, currentCode)) {
                is NetworkResult.Success -> {
                    _uiState.value = MfaVerificationUiState.Success
                }
                is NetworkResult.Error -> {
                    _uiState.value = MfaVerificationUiState.Error(res.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}

