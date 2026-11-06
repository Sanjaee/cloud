package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShareResourceItemDto(
    val resourceType: String, // "file" or "folder"
    val resourceId: String
)

@Serializable
data class ShareRequestDto(
    val resourceType: String,
    val resourceId: String,
    val recipientEmailOrUsername: String,
    val permission: String = "viewer", // "viewer", "commenter", "editor"
    val expiresAt: String? = null,
    val message: String? = null
)

@Serializable
data class BatchShareRequestDto(
    val items: List<ShareResourceItemDto> = emptyList(),
    val fileIds: List<String> = emptyList(),
    val folderIds: List<String> = emptyList(),
    val recipientEmailOrUsername: String,
    val permission: String = "viewer",
    val expiresAt: String? = null,
    val message: String? = null
)

@Serializable
data class CollaboratorDto(
    val id: String,
    val userId: String,
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val permission: String = "viewer",
    val createdAt: String? = null
)

@Serializable
data class CollaboratorListResponseDto(
    val collaborators: List<CollaboratorDto> = emptyList()
)

@Serializable
data class SharedItemsResponseDto(
    val files: List<FileItemDto> = emptyList(),
    val folders: List<FolderItemDto> = emptyList()
)

