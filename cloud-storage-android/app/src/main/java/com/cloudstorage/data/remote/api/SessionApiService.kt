package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.SessionListResponseDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SessionApiService {
    @GET("sessions")
    suspend fun getSessions(): Response<ApiResponseDto<SessionListResponseDto>>

    @DELETE("sessions/{id}")
    suspend fun revokeSession(
        @Path("id") id: String
    ): Response<ApiResponseDto<Map<String, String>>>

    @POST("sessions/revoke-others")
    suspend fun revokeOtherSessions(): Response<ApiResponseDto<Map<String, String>>>
}

