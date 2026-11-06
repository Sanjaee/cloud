package com.cloudstorage.domain.repository

import android.net.Uri
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.UploadProgress
import kotlinx.coroutines.flow.Flow

interface UploadRepository {
    fun enqueueUpload(uri: Uri, folderId: String?): String
    fun observeUploadProgress(workId: String): Flow<UploadProgress?>
    suspend fun directUpload(
        name: String,
        mimeType: String,
        size: Long,
        bytes: ByteArray,
        folderId: String?
    ): NetworkResult<FileItem>
}

