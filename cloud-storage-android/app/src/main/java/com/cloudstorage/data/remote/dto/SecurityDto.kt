package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class SecurityEventDto(
    val id: String? = null,
    val eventType: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: String? = null
)

@Serializable
data class SecurityEventsResponseDto(
    val events: List<SecurityEventDto> = emptyList()
)

