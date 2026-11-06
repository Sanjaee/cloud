package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.TrashBatchActionRequestDto
import com.cloudstorage.data.remote.dto.TrashResponseDto
import retrofit2.Response
import retrofit2.http.*

interface TrashApiService {
    @GET("trash")
    suspend fun getTrash(): Response<ApiResponseDto<TrashResponseDto>>

    @POST("trash/restore/{type}/{id}")
    suspend fun restoreItem(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<Map<String, String>>>

    @POST("trash/restore/batch")
    suspend fun restoreBatch(
        @Body request: TrashBatchActionRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>

    @DELETE("trash/permanent/{type}/{id}")
    suspend fun deletePermanently(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<ApiResponseDto<Map<String, String>>>

    @POST("trash/permanent/batch")
    suspend fun deletePermanentlyBatch(
        @Body request: TrashBatchActionRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>

    @DELETE("trash/empty")
    suspend fun emptyTrash(): Response<ApiResponseDto<Map<String, String>>>
}

