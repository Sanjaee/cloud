package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ShareApiService {
    @POST("shares")
    suspend fun shareResource(
        @Body request: ShareRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>

    @POST("shares/batch")
    suspend fun batchShare(
        @Body request: BatchShareRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>

    @GET("shares/shared-with-me")
    suspend fun getSharedWithMe(): Response<ApiResponseDto<SharedItemsResponseDto>>

    @GET("shares/shared-by-me")
    suspend fun getSharedByMe(): Response<ApiResponseDto<SharedItemsResponseDto>>

    @GET("shares/{resourceType}/{resourceId}/collaborators")
    suspend fun getCollaborators(
        @Path("resourceType") resourceType: String,
        @Path("resourceId") resourceId: String
    ): Response<ApiResponseDto<CollaboratorListResponseDto>>

    @DELETE("shares/{resourceType}/{resourceId}/collaborators/{userId}")
    suspend fun revokeCollaborator(
        @Path("resourceType") resourceType: String,
        @Path("resourceId") resourceId: String,
        @Path("userId") userId: String
    ): Response<ApiResponseDto<Map<String, String>>>
}

