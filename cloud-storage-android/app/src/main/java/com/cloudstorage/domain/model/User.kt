package com.cloudstorage.domain.model

data class User(
    val id: String,
    val email: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val storageQuota: Long = 10737418240L,
    val usedStorage: Long = 0L,
    val role: String = "user",
    val isEmailVerified: Boolean = true,
    val createdAt: String? = null
)

