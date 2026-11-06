package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.DownloadResponseDto
import com.cloudstorage.data.remote.dto.PreviewResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface DownloadApiService {
    @GET("downloads/{fileId}")
    suspend fun getDownloadUrl(
        @Path("fileId") fileId: String
    ): Response<ApiResponseDto<DownloadResponseDto>>

    @GET("downloads/{fileId}/preview")
    suspend fun getPreviewUrl(
        @Path("fileId") fileId: String
    ): Response<ApiResponseDto<PreviewResponseDto>>

    @GET
    @Streaming
    suspend fun downloadDirect(
        @Url url: String
    ): Response<ResponseBody>
}

