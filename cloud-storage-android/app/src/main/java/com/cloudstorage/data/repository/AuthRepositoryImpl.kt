package com.cloudstorage.data.repository

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.core.security.SecureTokenStorage
import com.cloudstorage.core.security.SessionManager
import com.cloudstorage.data.remote.api.AuthApiService
import com.cloudstorage.data.remote.api.UserApiService
import com.cloudstorage.data.remote.dto.GoogleTokenRequestDto
import com.cloudstorage.data.remote.dto.MfaVerifyRequestDto
import com.cloudstorage.domain.model.User
import com.cloudstorage.domain.repository.AuthRepository
import com.cloudstorage.domain.repository.GoogleAuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val userApiService: UserApiService,
    private val secureTokenStorage: SecureTokenStorage,
    private val sessionManager: SessionManager,
    private val apiErrorHandler: ApiErrorHandler
) : AuthRepository {

    override suspend fun signInWithGoogleIdToken(idToken: String): GoogleAuthResult {
        return try {
            val deviceId = secureTokenStorage.getOrCreateDeviceId()
            val response = authApiService.signInWithGoogle(
                GoogleTokenRequestDto(
                    idToken = idToken,
                    deviceId = deviceId
                )
            )

            if (!response.isSuccessful || response.body() == null) {
                val errorMsg = apiErrorHandler.parseErrorMessage(response)
                return GoogleAuthResult.Error(errorMsg)
            }

            val body = response.body()!!
            val data = body.data ?: return GoogleAuthResult.Error("Empty response data from server")

            if (data.mfaRequired) {
                return GoogleAuthResult.MfaRequired(
                    mfaToken = data.mfaToken ?: "",
                    email = data.email
                )
            }

            val tokens = data.tokens
            val userDto = data.user

            if (tokens != null && userDto != null) {
                secureTokenStorage.saveAccessToken(tokens.accessToken)
                secureTokenStorage.saveRefreshToken(tokens.refreshToken)

                val domainUser = User(
                    id = userDto.id,
                    email = userDto.email,
                    username = userDto.username,
                    avatarUrl = userDto.avatarUrl,
                    storageQuota = userDto.storageQuota,
                    usedStorage = userDto.usedStorage,
                    role = userDto.role,
                    isEmailVerified = userDto.isEmailVerified,
                    createdAt = userDto.createdAt
                )
                secureTokenStorage.saveUser(
                    id = domainUser.id,
                    email = domainUser.email,
                    username = domainUser.username,
                    avatarUrl = domainUser.avatarUrl
                )

                GoogleAuthResult.Success(domainUser)
            } else {
                GoogleAuthResult.Error("Invalid authentication response format")
            }
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.localizedMessage ?: "Network error during authentication")
        }
    }

    override suspend fun verifyGoogleMfa(mfaToken: String, code: String): NetworkResult<Unit> {
        return try {
            val response = authApiService.verifyGoogleMfa(
                MfaVerifyRequestDto(mfaToken = mfaToken, code = code)
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                val tokens = data.tokens
                if (tokens != null) {
                    secureTokenStorage.saveTokens(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        expiresInSec = tokens.expiresIn
                    )
                }
                val userDto = data.user
                if (userDto != null) {
                    secureTokenStorage.saveUser(
                        id = userDto.id,
                        email = userDto.email,
                        username = userDto.username,
                        avatarUrl = userDto.avatarUrl
                    )
                }
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(message = msg, statusCode = response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "MFA verification failed")
        }
    }

    override suspend fun getCurrentUser(): NetworkResult<User> {
        return try {
            val response = userApiService.getCurrentUser()
            if (response.isSuccessful && response.body()?.data != null) {
                val u = response.body()!!.data!!
                val domainUser = User(
                    id = u.id,
                    email = u.email,
                    username = u.username,
                    avatarUrl = u.avatarUrl,
                    storageQuota = u.storageQuota,
                    usedStorage = u.usedStorage,
                    role = u.role,
                    isEmailVerified = u.isEmailVerified,
                    createdAt = u.createdAt
                )
                secureTokenStorage.saveUser(
                    id = domainUser.id,
                    email = domainUser.email,
                    username = domainUser.username,
                    avatarUrl = domainUser.avatarUrl
                )
                NetworkResult.Success(domainUser)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(message = msg, statusCode = response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch user profile")
        }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        return try {
            authApiService.logout()
            secureTokenStorage.clearAll()
            sessionManager.notifySessionExpired()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            secureTokenStorage.clearAll()
            sessionManager.notifySessionExpired()
            NetworkResult.Success(Unit)
        }
    }

    override fun isAuthenticated(): Boolean {
        return secureTokenStorage.hasValidSession()
    }

    override fun getStoredUser(): User? {
        val email = secureTokenStorage.getUserEmail() ?: return null
        return User(
            id = secureTokenStorage.getUserId() ?: "",
            email = email,
            username = secureTokenStorage.getUsername(),
            avatarUrl = secureTokenStorage.getAvatarUrl()
        )
    }

    override fun observeAuthState(): Flow<Boolean> {
        return secureTokenStorage.isLoggedInFlow
    }
}

