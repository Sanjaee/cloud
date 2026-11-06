package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.User
import kotlinx.coroutines.flow.Flow

sealed interface GoogleAuthResult {
    data class Success(val user: User) : GoogleAuthResult
    data class MfaRequired(val mfaToken: String, val email: String?) : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}

interface AuthRepository {
    suspend fun signInWithGoogleIdToken(idToken: String): GoogleAuthResult
    suspend fun verifyGoogleMfa(mfaToken: String, code: String): NetworkResult<Unit>
    suspend fun getCurrentUser(): NetworkResult<User>
    suspend fun logout(): NetworkResult<Unit>
    fun isAuthenticated(): Boolean
    fun getStoredUser(): User?
    fun observeAuthState(): Flow<Boolean>
}

