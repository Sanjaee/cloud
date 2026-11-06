package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.Breadcrumb
import com.cloudstorage.domain.model.FolderItem
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getFoldersFlow(parentId: String?): Flow<List<FolderItem>>
    suspend fun refreshFolders(parentId: String?): NetworkResult<List<FolderItem>>
    suspend fun createFolder(name: String, parentId: String?, color: String? = null): NetworkResult<FolderItem>
    suspend fun getBreadcrumbs(folderId: String): NetworkResult<List<Breadcrumb>>
    suspend fun renameFolder(folderId: String, newName: String): NetworkResult<FolderItem>
    suspend fun moveFolder(folderId: String, targetParentId: String?): NetworkResult<FolderItem>
    suspend fun starFolder(folderId: String, isStarred: Boolean): NetworkResult<FolderItem>
    suspend fun deleteFolder(folderId: String): NetworkResult<Unit>
}

