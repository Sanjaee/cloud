package com.cloudstorage.ios.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean = false,
    val message: String? = null,
    val mfaRequired: Boolean = false,
    val data: T? = null,
    val error: ApiErrorDto? = null
)

@Serializable
data class ApiErrorDto(
    val code: String = "ERROR",
    val message: String = "An error occurred",
    val details: JsonElement? = null
)

@Serializable
data class TokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 900L
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val storageQuota: Long = 10737418240L,
    val usedStorage: Long = 0L,
    val storageUsed: Long = 0L,
    val role: String = "user",
    val isEmailVerified: Boolean = true,
    val createdAt: String? = null
)

@Serializable
data class AuthResponseDataDto(
    val user: UserDto? = null,
    val tokens: TokensDto? = null,
    val mfaRequired: Boolean = false,
    val mfaToken: String? = null,
    val email: String? = null,
    val ticket: String? = null
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val mfaCode: String? = null,
    val deviceId: String = "ios-kmp-device",
    val deviceName: String = "ZacodeCloud-iOS"
)

@Serializable
data class ExchangeTicketRequestDto(
    val ticket: String
)

@Serializable
data class MfaVerifyRequestDto(
    val mfaToken: String,
    val code: String
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)

@Serializable
data class FileItemDto(
    val id: String,
    val ownerId: String? = null,
    val folderId: String? = null,
    val storageObjectId: String? = null,
    val name: String,
    val originalName: String? = null,
    val mimeType: String = "application/octet-stream",
    val size: Long = 0L,
    val isStarred: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
)

@Serializable
data class FileListResponseDto(
    val files: List<FileItemDto> = emptyList()
)

@Serializable
data class FolderItemDto(
    val id: String,
    val ownerId: String? = null,
    val parentId: String? = null,
    val name: String,
    val color: String? = null,
    val isStarred: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
)

@Serializable
data class FolderListResponseDto(
    val folders: List<FolderItemDto> = emptyList()
)

@Serializable
data class FolderCreateRequestDto(
    val name: String,
    val parentId: String? = null,
    val color: String? = "#3B82F6"
)

@Serializable
data class RenameRequestDto(
    val name: String
)

@Serializable
data class BatchDeleteRequestDto(
    val fileIds: List<String> = emptyList(),
    val folderIds: List<String> = emptyList()
)

@Serializable
data class BreadcrumbDto(
    val id: String? = null,
    val name: String
)

@Serializable
data class BreadcrumbsResponseDto(
    val breadcrumbs: List<BreadcrumbDto> = emptyList()
)

@Serializable
data class QuotaResponseDto(
    val totalQuota: Long = 10737418240L,
    val usedStorage: Long = 0L,
    val availableStorage: Long = 10737418240L,
    val percentageUsed: Double = 0.0,
    val fileCount: Int = 0
)

@Serializable
data class ShareResourceItemDto(
    val resourceType: String,
    val resourceId: String
)

@Serializable
data class ShareRequestDto(
    val resourceType: String,
    val resourceId: String,
    val recipientEmailOrUsername: String,
    val permission: String = "viewer",
    val message: String? = null
)

@Serializable
data class BatchShareRequestDto(
    val items: List<ShareResourceItemDto> = emptyList(),
    val fileIds: List<String> = emptyList(),
    val folderIds: List<String> = emptyList(),
    val recipientEmailOrUsername: String,
    val permission: String = "viewer",
    val message: String? = null
)

@Serializable
data class CollaboratorDto(
    val id: String? = null,
    val permissionId: String? = null,
    val userId: String,
    val username: String? = null,
    val email: String? = null,
    val permission: String = "viewer",
    val createdAt: String? = null
)

@Serializable
data class CollaboratorListResponseDto(
    val collaborators: List<CollaboratorDto> = emptyList()
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

@Serializable
data class SharedItemsResponseDto(
    val files: List<FileItemDto> = emptyList(),
    val folders: List<FolderItemDto> = emptyList()
)

@Serializable
data class TrashResponseDto(
    val files: List<FileItemDto> = emptyList(),
    val folders: List<FolderItemDto> = emptyList()
)

@Serializable
data class UploadInitiateRequestDto(
    val filename: String,
    val size: Long,
    val mimeType: String = "application/octet-stream",
    val folderId: String? = null
)

@Serializable
data class UploadInitiateResponseDto(
    val uploadSessionId: String,
    val fileId: String? = null,
    val uploadType: String? = "single",
    val uploadUrl: String? = null
)

@Serializable
data class UploadCompleteRequestDto(
    val uploadSessionId: String
)

@Serializable
data class DownloadResponseDto(
    val downloadUrl: String,
    val name: String = "",
    val mimeType: String = "application/octet-stream",
    val size: Long = 0L
)

@Serializable
data class PreviewResponseDto(
    val previewUrl: String,
    val mimeType: String = "application/octet-stream"
)

@Serializable
data class SessionItemDto(
    val id: String,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    val isCurrent: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class SessionListResponseDto(
    val sessions: List<SessionItemDto> = emptyList()
)

@Serializable
data class SecurityEventDto(
    val id: String? = null,
    val eventType: String,
    val ipAddress: String? = null,
    val createdAt: String? = null
)

@Serializable
data class SecurityEventsResponseDto(
    val events: List<SecurityEventDto> = emptyList()
)

@Serializable
data class MfaStatusDto(
    val isEnabled: Boolean = false
)

@Serializable
data class MfaSetupResponseDto(
    val secret: String,
    val qrCodeDataUrl: String = "",
    val otpauthUrl: String? = null
)

@Serializable
data class MfaEnableRequestDto(
    val code: String,
    val totpCode: String? = null
)

@Serializable
data class MfaEnableResponseDto(
    val backupCodes: List<String> = emptyList()
)

@Serializable
data class MfaDisableRequestDto(
    val code: String,
    val totpCode: String? = null
)

