package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/google/token")
    suspend fun signInWithGoogle(
        @Body request: GoogleTokenRequestDto
    ): Response<ApiResponseDto<AuthResponseDataDto>>

    @POST("auth/google/mfa-verify")
    suspend fun verifyGoogleMfa(
        @Body request: MfaVerifyRequestDto
    ): Response<ApiResponseDto<AuthResponseDataDto>>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto
    ): Response<ApiResponseDto<TokensDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponseDto<Map<String, String>>>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<ApiResponseDto<Map<String, String>>>
}

