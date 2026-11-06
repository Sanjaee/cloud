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
    val id: String? = null,
    val permissionId: String? = null,
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
data class SharedByMeFileDto(
    val fileId: String,
    val fileName: String,
    val size: Long = 0L,
    val mimeType: String = "application/octet-stream",
    val permission: String = "viewer",
    val grantedToUsername: String? = null,
    val grantedToEmail: String? = null,
    val grantedAt: String? = null
)

@Serializable
data class SharedByMeFolderDto(
    val folderId: String,
    val folderName: String,
    val permission: String = "viewer",
    val grantedToUsername: String? = null,
    val grantedToEmail: String? = null,
    val grantedAt: String? = null
)

@Serializable
data class SharedItemsResponseDto(
    val files: List<FileItemDto> = emptyList(),
    val folders: List<FolderItemDto> = emptyList(),
    val sharedFiles: List<SharedByMeFileDto> = emptyList(),
    val sharedFolders: List<SharedByMeFolderDto> = emptyList()
)

@Serializable
data class CreatePublicLinkRequestDto(
    val resourceType: String,
    val resourceId: String,
    val permission: String = "viewer"
)

@Serializable
data class PublicLinkDataDto(
    val id: String? = null,
    val token: String,
    val permission: String = "viewer"
)

@Serializable
data class UserSearchItemDto(
    val id: String,
    val email: String,
    val username: String
)

@Serializable
data class UserSearchResponseDto(
    val users: List<UserSearchItemDto> = emptyList()
)
