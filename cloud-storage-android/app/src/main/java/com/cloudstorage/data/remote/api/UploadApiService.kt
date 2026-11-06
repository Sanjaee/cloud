package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface UploadApiService {
    @POST("uploads/initiate")
    suspend fun initiateUpload(
        @Body request: UploadInitiateRequestDto
    ): Response<ApiResponseDto<UploadInitiateResponseDto>>

    @POST("uploads/multipart/part-url")
    suspend fun getPartUrl(
        @Body request: UploadPartUrlRequestDto
    ): Response<ApiResponseDto<UploadPartUrlResponseDto>>

    @POST("uploads/complete")
    suspend fun completeUpload(
        @Body request: UploadCompleteRequestDto
    ): Response<ApiResponseDto<UploadCompleteResponseDto>>

    @PUT
    suspend fun uploadToPresignedUrl(
        @Url url: String,
        @Header("Content-Type") contentType: String,
        @Body content: RequestBody
    ): Response<Unit>
}
