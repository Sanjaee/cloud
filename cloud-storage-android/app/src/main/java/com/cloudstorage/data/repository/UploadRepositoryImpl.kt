package com.cloudstorage.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.asFlow
import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.core.worker.UploadWorker
import com.cloudstorage.core.worker.WorkerScheduler
import com.cloudstorage.data.remote.api.UploadApiService
import com.cloudstorage.data.remote.dto.UploadCompleteRequestDto
import com.cloudstorage.data.remote.dto.UploadInitiateRequestDto
import com.cloudstorage.data.remote.dto.UploadPartCompleteDto
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.UploadProgress
import com.cloudstorage.domain.model.UploadStatus
import com.cloudstorage.domain.repository.UploadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadApiService: UploadApiService,
    private val workerScheduler: WorkerScheduler,
    private val apiErrorHandler: ApiErrorHandler
) : UploadRepository {

    override fun enqueueUpload(uri: Uri, folderId: String?): String {
        var fileName = "file_${System.currentTimeMillis()}"
        var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val workId = workerScheduler.scheduleUpload(uri, fileName, mimeType, folderId)
        return workId.toString()
    }

    override fun observeUploadProgress(workId: String): Flow<UploadProgress?> {
        val uuid = UUID.fromString(workId)
        return workerScheduler.getWorkInfoById(uuid).asFlow().map { info ->
            if (info == null) null
            else {
                val progress = info.progress.getInt(UploadWorker.KEY_PROGRESS, 0)
                val status = when (info.state) {
                    androidx.work.WorkInfo.State.ENQUEUED -> UploadStatus.QUEUED
                    androidx.work.WorkInfo.State.RUNNING -> UploadStatus.UPLOADING
                    androidx.work.WorkInfo.State.SUCCEEDED -> UploadStatus.COMPLETED
                    androidx.work.WorkInfo.State.FAILED -> UploadStatus.FAILED
                    androidx.work.WorkInfo.State.CANCELLED -> UploadStatus.CANCELLED
                    else -> UploadStatus.QUEUED
                }
                UploadProgress(
                    uploadId = workId,
                    fileName = "",
                    bytesUploaded = progress.toLong(),
                    totalBytes = 100L,
                    status = status
                )
            }
        }
    }

    override suspend fun directUpload(
        name: String,
        mimeType: String,
        size: Long,
        bytes: ByteArray,
        folderId: String?
    ): NetworkResult<FileItem> {
        return try {
            val initRes = uploadApiService.initiateUpload(
                UploadInitiateRequestDto(filename = name, size = size, mimeType = mimeType, folderId = folderId)
            )
            if (!initRes.isSuccessful || initRes.body()?.data == null) {
                return NetworkResult.Error(apiErrorHandler.parseErrorMessage(initRes), initRes.code())
            }

            val initData = initRes.body()!!.data!!
            val uploadUrl = initData.uploadUrl
            if (uploadUrl.isNullOrEmpty()) {
                return NetworkResult.Error("Missing presigned upload URL from storage service")
            }

            val requestBody = bytes.toRequestBody(null)
            val putRes = uploadApiService.uploadToPresignedUrl(uploadUrl, mimeType, requestBody)
            if (!putRes.isSuccessful) {
                return NetworkResult.Error("Failed to upload binary payload to storage: ${putRes.code()}", putRes.code())
            }

            val compRes = uploadApiService.completeUpload(
                UploadCompleteRequestDto(
                    uploadSessionId = initData.uploadSessionId,
                    parts = null
                )
            )

            if (compRes.isSuccessful && compRes.body()?.data != null) {
                val dto = compRes.body()!!.data!!.file
                val file = FileItem(
                    id = dto.id,
                    ownerId = dto.ownerId,
                    folderId = dto.folderId,
                    storageObjectId = dto.storageObjectId,
                    name = dto.name,
                    originalName = dto.originalName,
                    mimeType = dto.mimeType,
                    size = dto.size,
                    isStarred = dto.isStarred,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt
                )
                NetworkResult.Success(file)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(compRes), compRes.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Direct upload failed")
        }
    }
}

