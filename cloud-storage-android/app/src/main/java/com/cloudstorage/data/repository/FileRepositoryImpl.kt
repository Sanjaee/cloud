package com.cloudstorage.data.repository

import com.cloudstorage.core.database.dao.FileDao
import com.cloudstorage.core.database.entity.FileEntity
import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.FileApiService
import com.cloudstorage.data.remote.dto.BatchDeleteRequestDto
import com.cloudstorage.data.remote.dto.FileMoveRequestDto
import com.cloudstorage.data.remote.dto.FileRenameRequestDto
import com.cloudstorage.data.remote.dto.FileStarRequestDto
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileApiService: FileApiService,
    private val fileDao: FileDao,
    private val apiErrorHandler: ApiErrorHandler
) : FileRepository {

    override fun getFilesFlow(folderId: String?): Flow<List<FileItem>> {
        return fileDao.getFilesByFolder(folderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshFiles(folderId: String?): NetworkResult<List<FileItem>> {
        return try {
            val response = fileApiService.getFiles(folderId)
            if (response.isSuccessful && response.body()?.data != null) {
                val dtos = response.body()!!.data!!.files
                val domainFiles = dtos.map { dto ->
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

                // Update local Room database cache
                fileDao.clearFilesForFolder(folderId)
                fileDao.insertFiles(domainFiles.map { FileEntity.fromDomain(it) })

                NetworkResult.Success(domainFiles)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load files")
        }
    }

    override suspend fun searchFiles(query: String): NetworkResult<List<FileItem>> {
        return try {
            val response = fileApiService.searchFiles(query)
            if (response.isSuccessful && response.body()?.data != null) {
                val dtos = response.body()!!.data!!.files
                val items = dtos.map { dto ->
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
                NetworkResult.Success(items)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Search failed")
        }
    }

    override suspend fun getFileDetail(fileId: String): NetworkResult<FileItem> {
        val cached = fileDao.getFileById(fileId)?.toDomain()
        return try {
            val response = fileApiService.getFileDetail(fileId)
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!.file
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
                fileDao.insertFile(FileEntity.fromDomain(file))
                NetworkResult.Success(file)
            } else if (cached != null) {
                NetworkResult.Success(cached)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (cached != null) {
                NetworkResult.Success(cached)
            } else {
                NetworkResult.Error(e.localizedMessage ?: "Failed to get file detail")
            }
        }
    }

    override suspend fun renameFile(fileId: String, newName: String): NetworkResult<FileItem> {
        return try {
            val response = fileApiService.renameFile(fileId, FileRenameRequestDto(name = newName))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
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
                fileDao.insertFile(FileEntity.fromDomain(file))
                NetworkResult.Success(file)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to rename file")
        }
    }

    override suspend fun moveFile(fileId: String, targetFolderId: String?): NetworkResult<FileItem> {
        return try {
            val response = fileApiService.moveFile(fileId, FileMoveRequestDto(targetFolderId = targetFolderId))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
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
                fileDao.insertFile(FileEntity.fromDomain(file))
                NetworkResult.Success(file)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to move file")
        }
    }

    override suspend fun starFile(fileId: String, isStarred: Boolean): NetworkResult<FileItem> {
        return try {
            val response = fileApiService.starFile(fileId, FileStarRequestDto(isStarred = isStarred))
            if (response.isSuccessful && response.body()?.data != null) {
                val dto = response.body()!!.data!!
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
                fileDao.insertFile(FileEntity.fromDomain(file))
                NetworkResult.Success(file)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to star file")
        }
    }

    override suspend fun deleteFile(fileId: String): NetworkResult<Unit> {
        return try {
            val response = fileApiService.deleteFile(fileId)
            if (response.isSuccessful) {
                fileDao.deleteFileById(fileId)
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to delete file")
        }
    }

    override suspend fun batchDelete(fileIds: List<String>, folderIds: List<String>): NetworkResult<Unit> {
        return try {
            val response = fileApiService.batchDelete(BatchDeleteRequestDto(fileIds = fileIds, folderIds = folderIds))
            if (response.isSuccessful) {
                fileDao.deleteFilesByIds(fileIds)
                NetworkResult.Success(Unit)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to delete items")
        }
    }
}

