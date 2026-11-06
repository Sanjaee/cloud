package com.cloudstorage.data.repository

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.TrashApiService
import com.cloudstorage.data.remote.dto.TrashBatchActionRequestDto
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.TrashRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashRepositoryImpl @Inject constructor(
    private val trashApiService: TrashApiService,
    private val apiErrorHandler: ApiErrorHandler
) : TrashRepository {

    override suspend fun getTrash(): NetworkResult<Pair<List<FileItem>, List<FolderItem>>> {
        return try {
            val response = trashApiService.getTrash()
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                val files = data.files.map { dto ->
                    FileItem(
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
                }
                val folders = data.folders.map { dto ->
                    FolderItem(
                        id = dto.id,
                        ownerId = dto.ownerId,
                        parentId = dto.parentId,
                        name = dto.name,
                        color = dto.color,
                        isStarred = dto.isStarred,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        deletedAt = dto.deletedAt
                    )
                }
                NetworkResult.Success(Pair(files, folders))
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load trash")
        }
    }

    override suspend fun restoreItem(type: String, id: String): NetworkResult<Unit> {
        return try {
            val response = trashApiService.restoreItem(type, id)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to restore item")
        }
    }

    override suspend fun restoreBatch(fileIds: List<String>, folderIds: List<String>): NetworkResult<Unit> {
        return try {
            val response = trashApiService.restoreBatch(TrashBatchActionRequestDto(fileIds, folderIds))
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to restore items")
        }
    }

    override suspend fun deletePermanently(type: String, id: String): NetworkResult<Unit> {
        return try {
            val response = trashApiService.deletePermanently(type, id)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to delete item permanently")
        }
    }

    override suspend fun deletePermanentlyBatch(fileIds: List<String>, folderIds: List<String>): NetworkResult<Unit> {
        return try {
            val response = trashApiService.deletePermanentlyBatch(TrashBatchActionRequestDto(fileIds, folderIds))
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to delete items permanently")
        }
    }

    override suspend fun emptyTrash(): NetworkResult<Unit> {
        return try {
            val response = trashApiService.emptyTrash()
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to empty trash")
        }
    }
}

