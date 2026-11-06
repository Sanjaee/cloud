package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MfaApiService {
    @GET("mfa/status")
    suspend fun getMfaStatus(): Response<ApiResponseDto<MfaStatusDto>>

    @POST("mfa/setup")
    suspend fun setupMfa(
        @Body request: MfaSetupRequestDto = MfaSetupRequestDto()
    ): Response<ApiResponseDto<MfaSetupResponseDto>>

    @POST("mfa/enable")
    suspend fun enableMfa(
        @Body request: MfaEnableRequestDto
    ): Response<ApiResponseDto<MfaEnableResponseDto>>

    @POST("mfa/disable")
    suspend fun disableMfa(
        @Body request: MfaDisableRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>
}

