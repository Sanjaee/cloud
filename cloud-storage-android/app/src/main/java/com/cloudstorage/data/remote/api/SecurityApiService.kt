package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.ChangePasswordRequestDto
import com.cloudstorage.data.remote.dto.SecurityEventsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SecurityApiService {
    @POST("security/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>

    @GET("security/events")
    suspend fun getSecurityEvents(): Response<ApiResponseDto<SecurityEventsResponseDto>>
}

