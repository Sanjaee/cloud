package com.cloudstorage.feature.auth.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.BuildConfig
import com.cloudstorage.domain.repository.AuthRepository
import com.cloudstorage.domain.repository.GoogleAuthResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GoogleLoginUiState {
    object Idle : GoogleLoginUiState
    object Loading : GoogleLoginUiState
    object Success : GoogleLoginUiState
    data class MfaRequired(val mfaToken: String, val email: String?) : GoogleLoginUiState
    data class Error(val message: String) : GoogleLoginUiState
}

@HiltViewModel
class GoogleLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GoogleLoginUiState>(GoogleLoginUiState.Idle)
    val uiState: StateFlow<GoogleLoginUiState> = _uiState.asStateFlow()

    // Google Web Client ID synchronized with backend OAuth (GOOGLE_CLIENT_ID)
    private val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = GoogleLoginUiState.Loading

            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // Send verified Google ID Token to Backend API
                    when (val authRes = authRepository.signInWithGoogleIdToken(idToken)) {
                        is GoogleAuthResult.Success -> {
                            _uiState.value = GoogleLoginUiState.Success
                        }
                        is GoogleAuthResult.MfaRequired -> {
                            _uiState.value = GoogleLoginUiState.MfaRequired(
                                mfaToken = authRes.mfaToken,
                                email = authRes.email
                            )
                        }
                        is GoogleAuthResult.Error -> {
                            _uiState.value = GoogleLoginUiState.Error(authRes.message)
                        }
                    }
                } else {
                    _uiState.value = GoogleLoginUiState.Error("Unsupported credential type received")
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed account picker
                _uiState.value = GoogleLoginUiState.Idle
            } catch (e: GetCredentialException) {
                _uiState.value = GoogleLoginUiState.Error(e.localizedMessage ?: "Google Sign-In failed")
            } catch (e: Exception) {
                _uiState.value = GoogleLoginUiState.Error(e.localizedMessage ?: "An unexpected error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = GoogleLoginUiState.Idle
    }
}

