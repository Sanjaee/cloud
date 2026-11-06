package com.cloudstorage.core.worker

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cloudstorage.data.remote.api.DownloadApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadApiService: DownloadApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val fileId = inputData.getString(KEY_FILE_ID) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "download_${System.currentTimeMillis()}"

        return try {
            setProgress(workDataOf(KEY_PROGRESS to 10))

            val downloadUrlRes = downloadApiService.getDownloadUrl(fileId)
            if (!downloadUrlRes.isSuccessful || downloadUrlRes.body()?.data == null) {
                return Result.failure()
            }

            val downloadUrl = downloadUrlRes.body()!!.data!!.downloadUrl
            setProgress(workDataOf(KEY_PROGRESS to 30))

            val responseBodyRes = downloadApiService.downloadDirect(downloadUrl)
            if (!responseBodyRes.isSuccessful || responseBodyRes.body() == null) {
                return Result.failure()
            }

            val body = responseBodyRes.body()!!
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val targetFile = File(downloadsDir, fileName)
            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            setProgress(workDataOf(KEY_PROGRESS to 100))
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_FILE_ID = "key_file_id"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_PROGRESS = "key_progress"
    }
}

