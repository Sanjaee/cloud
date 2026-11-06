package com.cloudstorage.data.repository

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.core.worker.WorkerScheduler
import com.cloudstorage.data.remote.api.DownloadApiService
import com.cloudstorage.domain.repository.DownloadRepository
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadApiService: DownloadApiService,
    private val workerScheduler: WorkerScheduler,
    private val apiErrorHandler: ApiErrorHandler
) : DownloadRepository {

    override suspend fun getDownloadUrl(fileId: String): NetworkResult<String> {
        return try {
            val response = downloadApiService.getDownloadUrl(fileId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!.downloadUrl)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to get download URL")
        }
    }

    override suspend fun getPreviewUrl(fileId: String): NetworkResult<String> {
        return try {
            val response = downloadApiService.getPreviewUrl(fileId)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!.previewUrl)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to get preview URL")
        }
    }

    override fun enqueueDownload(fileId: String, fileName: String): String {
        val workId = workerScheduler.scheduleDownload(fileId, fileName)
        return workId.toString()
    }

    override suspend fun downloadToFile(fileId: String, destinationFile: File): NetworkResult<File> {
        return try {
            val urlRes = downloadApiService.getDownloadUrl(fileId)
            if (!urlRes.isSuccessful || urlRes.body()?.data == null) {
                return NetworkResult.Error(apiErrorHandler.parseErrorMessage(urlRes), urlRes.code())
            }

            val downloadUrl = urlRes.body()!!.data!!.downloadUrl
            val streamRes = downloadApiService.downloadDirect(downloadUrl)
            if (!streamRes.isSuccessful || streamRes.body() == null) {
                return NetworkResult.Error("Failed to stream download", streamRes.code())
            }

            val body = streamRes.body()!!
            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            NetworkResult.Success(destinationFile)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Direct download failed")
        }
    }
}

