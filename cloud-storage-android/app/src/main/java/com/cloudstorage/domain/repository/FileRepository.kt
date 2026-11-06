package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFilesFlow(folderId: String?): Flow<List<FileItem>>
    suspend fun refreshFiles(folderId: String?): NetworkResult<List<FileItem>>
    suspend fun searchFiles(query: String): NetworkResult<List<FileItem>>
    suspend fun getFileDetail(fileId: String): NetworkResult<FileItem>
    suspend fun renameFile(fileId: String, newName: String): NetworkResult<FileItem>
    suspend fun moveFile(fileId: String, targetFolderId: String?): NetworkResult<FileItem>
    suspend fun starFile(fileId: String, isStarred: Boolean): NetworkResult<FileItem>
    suspend fun deleteFile(fileId: String): NetworkResult<Unit>
    suspend fun batchDelete(fileIds: List<String>, folderIds: List<String>): NetworkResult<Unit>
}

