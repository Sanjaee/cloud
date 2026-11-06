package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface FolderApiService {
    @GET("folders")
    suspend fun getFolders(
        @Query("parentId") parentId: String? = null
    ): Response<ApiResponseDto<FolderListResponseDto>>

    @POST("folders")
    suspend fun createFolder(
        @Body request: FolderCreateRequestDto
    ): Response<ApiResponseDto<FolderItemDto>>

    @GET("folders/{folderId}/breadcrumbs")
    suspend fun getBreadcrumbs(
        @Path("folderId") folderId: String
    ): Response<ApiResponseDto<BreadcrumbsResponseDto>>

    @PATCH("folders/{folderId}/rename")
    suspend fun renameFolder(
        @Path("folderId") folderId: String,
        @Body request: FolderRenameRequestDto
    ): Response<ApiResponseDto<FolderItemDto>>

    @PATCH("folders/{folderId}/move")
    suspend fun moveFolder(
        @Path("folderId") folderId: String,
        @Body request: FolderMoveRequestDto
    ): Response<ApiResponseDto<FolderItemDto>>

    @PATCH("folders/{folderId}/star")
    suspend fun starFolder(
        @Path("folderId") folderId: String,
        @Body request: FolderStarRequestDto
    ): Response<ApiResponseDto<FolderItemDto>>

    @DELETE("folders/{folderId}")
    suspend fun deleteFolder(
        @Path("folderId") folderId: String
    ): Response<ApiResponseDto<Map<String, String>>>
}

