package com.cloudstorage.data.repository

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.ShareApiService
import com.cloudstorage.data.remote.dto.BatchShareRequestDto
import com.cloudstorage.data.remote.dto.CreatePublicLinkRequestDto
import com.cloudstorage.data.remote.dto.ShareRequestDto
import com.cloudstorage.data.remote.dto.ShareResourceItemDto
import com.cloudstorage.data.remote.dto.UserSearchItemDto
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
                    message = message?.takeIf { it.isNotBlank() }
                )
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Gagal membagikan item")
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
            val items = buildList {
                fileIds.forEach { add(ShareResourceItemDto("file", it)) }
                folderIds.forEach { add(ShareResourceItemDto("folder", it)) }
            }
            val response = shareApiService.batchShare(
                BatchShareRequestDto(
                    items = items,
                    fileIds = fileIds,
                    folderIds = folderIds,
                    recipientEmailOrUsername = recipient,
                    permission = permission,
                    message = message?.takeIf { it.isNotBlank() }
                )
            )
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Gagal membagikan beberapa item")
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
                val files = if (data.files.isNotEmpty()) {
                    data.files.map { dto ->
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
                } else {
                    data.sharedFiles.distinctBy { it.fileId }.map { dto ->
                        FileItem(
                            id = dto.fileId,
                            ownerId = null,
                            folderId = null,
                            storageObjectId = null,
                            name = dto.fileName,
                            originalName = dto.fileName,
                            mimeType = dto.mimeType,
                            size = dto.size,
                            isStarred = false,
                            createdAt = dto.grantedAt,
                            updatedAt = dto.grantedAt,
                            deletedAt = null
                        )
                    }
                }

                val folders = if (data.folders.isNotEmpty()) {
                    data.folders.map { dto ->
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
                } else {
                    data.sharedFolders.distinctBy { it.folderId }.map { dto ->
                        FolderItem(
                            id = dto.folderId,
                            ownerId = null,
                            parentId = null,
                            name = dto.folderName,
                            color = null,
                            isStarred = false,
                            createdAt = dto.grantedAt,
                            updatedAt = dto.grantedAt,
                            deletedAt = null
                        )
                    }
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
                        id = dto.id ?: dto.permissionId ?: dto.userId,
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

    override suspend fun createPublicLink(resourceType: String, resourceId: String): NetworkResult<String> {
        return try {
            val response = shareApiService.createPublicLink(
                CreatePublicLinkRequestDto(
                    resourceType = resourceType,
                    resourceId = resourceId,
                    permission = "viewer"
                )
            )
            if (response.isSuccessful && response.body()?.data?.token != null) {
                val token = response.body()!!.data!!.token
                NetworkResult.Success("https://cloud-storage-web.vercel.app/share/$token")
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Gagal membuat public link")
        }
    }

    override suspend fun searchUsers(query: String): NetworkResult<List<UserSearchItemDto>> {
        return try {
            val response = shareApiService.searchUsers(query)
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!.users)
            } else {
                NetworkResult.Success(emptyList())
            }
        } catch (_: Exception) {
            NetworkResult.Success(emptyList())
        }
    }
}
