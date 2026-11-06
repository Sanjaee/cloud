package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionItemDto(
    val id: String,
    val userId: String? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    val deviceType: String? = null,
    val isCurrent: Boolean = false,
    val createdAt: String? = null,
    val lastActiveAt: String? = null,
    val lastUsedAt: String? = null
)

@Serializable
data class SessionListResponseDto(
    val sessions: List<SessionItemDto> = emptyList()
)
