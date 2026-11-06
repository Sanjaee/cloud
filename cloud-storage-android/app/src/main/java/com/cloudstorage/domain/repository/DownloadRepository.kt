package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import java.io.File

interface DownloadRepository {
    suspend fun getDownloadUrl(fileId: String): NetworkResult<String>
    suspend fun getPreviewUrl(fileId: String): NetworkResult<String>
    fun enqueueDownload(fileId: String, fileName: String): String
    suspend fun downloadToFile(fileId: String, destinationFile: File): NetworkResult<File>
}

