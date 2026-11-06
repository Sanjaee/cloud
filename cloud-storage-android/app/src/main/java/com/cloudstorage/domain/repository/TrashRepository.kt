package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem

interface TrashRepository {
    suspend fun getTrash(): NetworkResult<Pair<List<FileItem>, List<FolderItem>>>
    suspend fun restoreItem(type: String, id: String): NetworkResult<Unit>
    suspend fun restoreBatch(fileIds: List<String>, folderIds: List<String>): NetworkResult<Unit>
    suspend fun deletePermanently(type: String, id: String): NetworkResult<Unit>
    suspend fun deletePermanentlyBatch(fileIds: List<String>, folderIds: List<String>): NetworkResult<Unit>
    suspend fun emptyTrash(): NetworkResult<Unit>
}

