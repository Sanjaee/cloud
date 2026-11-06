package com.cloudstorage.data.repository

import com.cloudstorage.core.database.dao.FolderDao
import com.cloudstorage.core.database.entity.FolderEntity
import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.FolderApiService
import com.cloudstorage.data.remote.dto.FolderCreateRequestDto
import com.cloudstorage.data.remote.dto.FolderMoveRequestDto
import com.cloudstorage.data.remote.dto.FolderRenameRequestDto
import com.cloudstorage.data.remote.dto.FolderStarRequestDto
import com.cloudstorage.domain.model.Breadcrumb
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderApiService: FolderApiService,
    private val folderDao: FolderDao,
    private val apiErrorHandler: ApiErrorHandler
) : FolderRepository {

    override fun getFoldersFlow(parentId: String?): Flow<List<FolderItem>> {
        return folderDao.getFoldersByParent(parentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshFolders(parentId: String?): NetworkResult<List<FolderItem>> {
        return try {
            val response = folderApiService.getFolders(parentId)
            if (response.isSuccessful && response.body()?.data != null) {
                val dtos = response.body()!!.data!!.folders
                val domainFolders = dtos.map { dto ->
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

                // Update Room cache
                folderDao.clearFoldersForParent(parentId)
                folderDao.insertFolders(domainFolders.map { FolderEntity.fromDomain(it) })

                NetworkResult.Success(domainFolders)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load folders")
        }
    }

    override suspend fun createFolder(name: String, parentId: String?, color: String?): NetworkResult<FolderItem> {
        return try {
            val response = folderApiService.createFolder(
                FolderCreateRequestDto(name = name, parentId = parentId, color = color)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                val folder = FolderItem(
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
                folderDao.insertFolder(FolderEntity.fromDomain(folder))
                NetworkResult.Success(folder)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to create folder")
        }
    }

    override suspend fun getBreadcrumbs(folderId: String): NetworkResult<List<Breadcrumb>> {
        return try {
            val response = folderApiService.getBreadcrumbs(folderId)
            if (response.isSuccessful && response.body()?.data != null) {
                val dtos = response.body()!!.data!!.breadcrumbs
                val breadcrumbs = dtos.map { Breadcrumb(id = it.id, name = it.name) }
                NetworkResult.Success(breadcrumbs)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load breadcrumbs")
        }
    }

    override suspend fun renameFolder(folderId: String, newName: String): NetworkResult<FolderItem> {
        return try {
            val response = folderApiService.renameFolder(folderId, FolderRenameRequestDto(name = newName))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                val folder = FolderItem(
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
                folderDao.insertFolder(FolderEntity.fromDomain(folder))
                NetworkResult.Success(folder)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to rename folder")
        }
    }

    override suspend fun moveFolder(folderId: String, targetParentId: String?): NetworkResult<FolderItem> {
        return try {
            val response = folderApiService.moveFolder(folderId, FolderMoveRequestDto(targetParentId = targetParentId))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                val folder = FolderItem(
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
                folderDao.insertFolder(FolderEntity.fromDomain(folder))
                NetworkResult.Success(folder)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to move folder")
        }
    }

    override suspend fun starFolder(folderId: String, isStarred: Boolean): NetworkResult<FolderItem> {
        return try {
            val response = folderApiService.starFolder(folderId, FolderStarRequestDto(isStarred = isStarred))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
                val folder = FolderItem(
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
                folderDao.insertFolder(FolderEntity.fromDomain(folder))
                NetworkResult.Success(folder)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to star folder")
        }
    }

    override suspend fun deleteFolder(folderId: String): NetworkResult<Unit> {
        return try {
            val response = folderApiService.deleteFolder(folderId)
            if (response.isSuccessful) {
                folderDao.deleteFolderById(folderId)
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to delete folder")
        }
    }
}

