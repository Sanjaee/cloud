package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface FileApiService {
    @GET("files")
    suspend fun getFiles(
        @Query("folderId") folderId: String? = null
    ): Response<ApiResponseDto<FileListResponseDto>>

    @GET("files/search")
    suspend fun searchFiles(
        @Query("q") query: String
    ): Response<ApiResponseDto<FileListResponseDto>>

    @GET("files/{fileId}")
    suspend fun getFileDetail(
        @Path("fileId") fileId: String
    ): Response<ApiResponseDto<FileDetailResponseDto>>

    @PATCH("files/{fileId}/rename")
    suspend fun renameFile(
        @Path("fileId") fileId: String,
        @Body request: FileRenameRequestDto
    ): Response<ApiResponseDto<FileItemDto>>

    @PATCH("files/{fileId}/move")
    suspend fun moveFile(
        @Path("fileId") fileId: String,
        @Body request: FileMoveRequestDto
    ): Response<ApiResponseDto<FileItemDto>>

    @PATCH("files/{fileId}/star")
    suspend fun starFile(
        @Path("fileId") fileId: String,
        @Body request: FileStarRequestDto
    ): Response<ApiResponseDto<FileItemDto>>

    @DELETE("files/{fileId}")
    suspend fun deleteFile(
        @Path("fileId") fileId: String
    ): Response<ApiResponseDto<Map<String, String>>>

    @POST("files/batch-delete")
    suspend fun batchDelete(
        @Body request: BatchDeleteRequestDto
    ): Response<ApiResponseDto<Map<String, String>>>
}

