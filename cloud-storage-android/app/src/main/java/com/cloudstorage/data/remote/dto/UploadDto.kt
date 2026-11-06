package com.cloudstorage.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadInitiateRequestDto(
    val filename: String,
    val size: Long,
    val mimeType: String = "application/octet-stream",
    val folderId: String? = null
)

@Serializable
data class UploadInitiateResponseDto(
    val uploadSessionId: String,
    val fileId: String? = null,
    val storageObjectId: String? = null,
    val uploadType: String? = "single",
    val uploadUrl: String? = null,
    val expiresIn: Int? = null,
    val uploadId: String? = null,
    val totalParts: Int? = null,
    val chunkSize: Long? = null
)

@Serializable
data class UploadPartUrlRequestDto(
    val uploadSessionId: String,
    val partNumber: Int
)

@Serializable
data class UploadPartUrlResponseDto(
    val uploadUrl: String,
    val partNumber: Int
)

@Serializable
data class UploadPartCompleteDto(
    @SerialName("PartNumber")
    val partNumber: Int,
    @SerialName("ETag")
    val etag: String
)

@Serializable
data class UploadCompleteRequestDto(
    val uploadSessionId: String,
    val parts: List<UploadPartCompleteDto>? = null
)

@Serializable
data class UploadCompleteResponseDto(
    val file: FileItemDto
)
