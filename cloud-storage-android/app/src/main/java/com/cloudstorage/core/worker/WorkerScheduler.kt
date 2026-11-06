package com.cloudstorage.core.worker

import android.content.Context
import android.net.Uri
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleUpload(
        uri: Uri,
        fileName: String,
        mimeType: String,
        folderId: String?
    ): UUID {
        val inputData = workDataOf(
            UploadWorker.KEY_URI to uri.toString(),
            UploadWorker.KEY_FILE_NAME to fileName,
            UploadWorker.KEY_MIME_TYPE to mimeType,
            UploadWorker.KEY_FOLDER_ID to folderId
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(request)
        return request.id
    }

    fun scheduleDownload(fileId: String, fileName: String): UUID {
        val inputData = workDataOf(
            DownloadWorker.KEY_FILE_ID to fileId,
            DownloadWorker.KEY_FILE_NAME to fileName
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(request)
        return request.id
    }

    fun getWorkInfoById(id: UUID) = workManager.getWorkInfoByIdLiveData(id)
}

