package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DownloadResponseDto(
    val downloadUrl: String,
    val name: String,
    val mimeType: String = "application/octet-stream",
    val size: Long = 0L
)

@Serializable
data class PreviewResponseDto(
    val previewUrl: String,
    val mimeType: String = "application/octet-stream"
)

