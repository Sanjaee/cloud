package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.Collaborator
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem

interface ShareRepository {
    suspend fun shareResource(
        resourceType: String,
        resourceId: String,
        recipient: String,
        permission: String,
        message: String? = null
    ): NetworkResult<Unit>

    suspend fun batchShare(
        fileIds: List<String>,
        folderIds: List<String>,
        recipient: String,
        permission: String,
        message: String? = null
    ): NetworkResult<Unit>

    suspend fun getSharedWithMe(): NetworkResult<Pair<List<FileItem>, List<FolderItem>>>
    suspend fun getSharedByMe(): NetworkResult<Pair<List<FileItem>, List<FolderItem>>>
    suspend fun getCollaborators(resourceType: String, resourceId: String): NetworkResult<List<Collaborator>>
    suspend fun revokeCollaborator(resourceType: String, resourceId: String, userId: String): NetworkResult<Unit>
}

