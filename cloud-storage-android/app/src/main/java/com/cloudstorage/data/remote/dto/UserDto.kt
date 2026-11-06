package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val storageQuota: Long = 10737418240L, // 10 GB default
    val usedStorage: Long = 0L,
    val role: String = "user",
    val isEmailVerified: Boolean = true,
    val createdAt: String? = null
)

@Serializable
data class UserProfileUpdateRequestDto(
    val username: String? = null,
    val avatarUrl: String? = null
)

