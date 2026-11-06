package com.cloudstorage.data.repository

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.ShareApiService
import com.cloudstorage.data.remote.dto.BatchShareRequestDto
import com.cloudstorage.data.remote.dto.ShareRequestDto
import com.cloudstorage.domain.model.Collaborator
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.ShareRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepositoryImpl @Inject constructor(
    private val shareApiService: ShareApiService,
    private val apiErrorHandler: ApiErrorHandler
) : ShareRepository {

    override suspend fun shareResource(
        resourceType: String,
        resourceId: String,
        recipient: String,
        permission: String,
        message: String?
    ): NetworkResult<Unit> {
        return try {
            val response = shareApiService.shareResource(
                ShareRequestDto(
                    resourceType = resourceType,
                    resourceId = resourceId,
                    recipientEmailOrUsername = recipient,
                    permission = permission,
                    message = message
                )
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to share resource")
        }
    }

    override suspend fun batchShare(
        fileIds: List<String>,
        folderIds: List<String>,
        recipient: String,
        permission: String,
        message: String?
    ): NetworkResult<Unit> {
        return try {
            val response = shareApiService.batchShare(
                BatchShareRequestDto(
                    fileIds = fileIds,
                    folderIds = folderIds,
                    recipientEmailOrUsername = recipient,
                    permission = permission,
                    message = message
                )
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to batch share resources")
        }
    }

    override suspend fun getSharedWithMe(): NetworkResult<Pair<List<FileItem>, List<FolderItem>>> {
        return try {
            val response = shareApiService.getSharedWithMe()
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
            NetworkResult.Error(e.localizedMessage ?: "Failed to load shared items")
        }
    }

    override suspend fun getSharedByMe(): NetworkResult<Pair<List<FileItem>, List<FolderItem>>> {
        return try {
            val response = shareApiService.getSharedByMe()
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
            NetworkResult.Error(e.localizedMessage ?: "Failed to load shared items")
        }
    }

    override suspend fun getCollaborators(resourceType: String, resourceId: String): NetworkResult<List<Collaborator>> {
        return try {
            val response = shareApiService.getCollaborators(resourceType, resourceId)
            if (response.isSuccessful && response.body()?.data != null) {
                val dtos = response.body()!!.data!!.collaborators
                val list = dtos.map { dto ->
                    Collaborator(
                        id = dto.id,
                        userId = dto.userId,
                        username = dto.username,
                        email = dto.email,
                        avatarUrl = dto.avatarUrl,
                        permission = dto.permission,
                        createdAt = dto.createdAt
                    )
                }
                NetworkResult.Success(list)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load collaborators")
        }
    }

    override suspend fun revokeCollaborator(
        resourceType: String,
        resourceId: String,
        userId: String
    ): NetworkResult<Unit> {
        return try {
            val response = shareApiService.revokeCollaborator(resourceType, resourceId, userId)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to revoke collaborator")
        }
    }
}

