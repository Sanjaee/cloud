package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.NotificationListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApiService {
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponseDto<NotificationListResponseDto>>

    @PATCH("notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: String
    ): Response<ApiResponseDto<Map<String, String>>>

    @POST("notifications/read-all")
    suspend fun markAllAsRead(): Response<ApiResponseDto<Map<String, String>>>
}

