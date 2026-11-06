package com.cloudstorage.core.network

import com.cloudstorage.BuildConfig
import com.cloudstorage.core.security.SecureTokenStorage
import com.cloudstorage.core.security.SessionManager
import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.RefreshTokenRequestDto
import com.cloudstorage.data.remote.dto.TokensDto
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val sessionManager: SessionManager,
    private val okHttpClientProvider: Provider<OkHttpClient>
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if refresh endpoint itself failed
        if (response.request.url.encodedPath.contains("/auth/refresh")) {
            sessionManager.notifySessionExpired()
            return null
        }

        // Only retry once per 401
        if (responseCount(response) >= 3) {
            sessionManager.notifySessionExpired()
            return null
        }

        synchronized(lock) {
            val currentAccessToken = tokenStorage.getAccessToken()
            val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")?.trim()

            // If another thread already refreshed the token, retry immediately with the new token
            if (currentAccessToken != null && currentAccessToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.notifySessionExpired()
                return null
            }

            // Perform synchronous refresh call
            val newTokens = performTokenRefresh(refreshToken)
            if (newTokens != null) {
                tokenStorage.saveTokens(
                    accessToken = newTokens.accessToken,
                    refreshToken = newTokens.refreshToken,
                    expiresInSec = newTokens.expiresIn
                )

                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                sessionManager.notifySessionExpired()
                return null
            }
        }
    }

    private fun performTokenRefresh(refreshToken: String): TokensDto? {
        return try {
            val refreshUrl = "${BuildConfig.BASE_URL}auth/refresh"
            val requestBodyJson = json.encodeToString(
                RefreshTokenRequestDto.serializer(),
                RefreshTokenRequestDto(refreshToken)
            )

            val request = Request.Builder()
                .url(refreshUrl)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "ZacodeStorage-Android/1.0.0")
                .build()

            // Use a clean OkHttpClient without AuthInterceptor to avoid recursion
            val cleanClient = OkHttpClient.Builder().build()
            val httpResponse = cleanClient.newCall(request).execute()

            if (httpResponse.isSuccessful) {
                val bodyString = httpResponse.body?.string() ?: return null
                val result = json.decodeFromString<ApiResponseDto<TokensDto>>(bodyString)
                result.data
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
