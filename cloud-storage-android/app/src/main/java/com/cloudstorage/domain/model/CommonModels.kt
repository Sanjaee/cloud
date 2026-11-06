package com.cloudstorage.domain.model

data class Collaborator(
    val id: String,
    val userId: String,
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val permission: String = "viewer",
    val createdAt: String? = null
)

data class NotificationItem(
    val id: String,
    val userId: String,
    val actorId: String? = null,
    val type: String,
    val resourceType: String? = null,
    val resourceId: String? = null,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: String? = null
)

data class SessionItem(
    val id: String,
    val userId: String? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    val deviceType: String? = null,
    val isCurrent: Boolean = false,
    val createdAt: String? = null,
    val lastActiveAt: String? = null
)

data class UploadProgress(
    val uploadId: String,
    val fileName: String,
    val bytesUploaded: Long,
    val totalBytes: Long,
    val status: UploadStatus
) {
    val progress: Float
        get() = if (totalBytes > 0) bytesUploaded.toFloat() / totalBytes.toFloat() else 0f
}

enum class UploadStatus {
    QUEUED,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

