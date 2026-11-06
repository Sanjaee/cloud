package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.QuotaResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface QuotaApiService {
    @GET("quotas")
    suspend fun getQuota(): Response<ApiResponseDto<QuotaResponseDto>>
}

