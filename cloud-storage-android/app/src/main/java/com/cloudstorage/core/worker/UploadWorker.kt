package com.cloudstorage.core.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cloudstorage.data.remote.api.UploadApiService
import com.cloudstorage.data.remote.dto.UploadCompleteRequestDto
import com.cloudstorage.data.remote.dto.UploadInitiateRequestDto
import com.cloudstorage.data.remote.dto.UploadPartCompleteDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val uploadApiService: UploadApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val folderId = inputData.getString(KEY_FOLDER_ID)
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "upload_${System.currentTimeMillis()}"
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "application/octet-stream"

        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Result.failure()
            val bytes = inputStream.use { it.readBytes() }
            val fileSize = bytes.size.toLong()

            setProgress(workDataOf(KEY_PROGRESS to 10))

            // 1. Initiate upload
            val initResponse = uploadApiService.initiateUpload(
                UploadInitiateRequestDto(
                    filename = fileName,
                    size = fileSize,
                    mimeType = mimeType,
                    folderId = folderId
                )
            )

            if (!initResponse.isSuccessful || initResponse.body()?.data == null) {
                return Result.failure()
            }

            val initData = initResponse.body()!!.data!!
            val uploadSessionId = initData.uploadSessionId
            val uploadUrl = initData.uploadUrl

            setProgress(workDataOf(KEY_PROGRESS to 30))

            // 2. Upload direct to Cloudflare R2 presigned URL
            if (!uploadUrl.isNullOrEmpty()) {
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)
                val putResponse = uploadApiService.uploadToPresignedUrl(uploadUrl, mimeType, requestBody)

                if (!putResponse.isSuccessful) {
                    return Result.failure()
                }

                setProgress(workDataOf(KEY_PROGRESS to 80))

                // 3. Complete upload
                val completeResponse = uploadApiService.completeUpload(
                    UploadCompleteRequestDto(
                        uploadSessionId = uploadSessionId,
                        parts = null
                    )
                )

                if (completeResponse.isSuccessful) {
                    setProgress(workDataOf(KEY_PROGRESS to 100))
                    Result.success()
                } else {
                    Result.failure()
                }
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_URI = "key_uri"
        const val KEY_FOLDER_ID = "key_folder_id"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_MIME_TYPE = "key_mime_type"
        const val KEY_PROGRESS = "key_progress"
    }
}

