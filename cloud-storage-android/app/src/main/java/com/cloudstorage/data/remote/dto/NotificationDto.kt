package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationItemDto(
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

@Serializable
data class NotificationListResponseDto(
    val notifications: List<NotificationItemDto> = emptyList(),
    val unreadCount: Int = 0
)

