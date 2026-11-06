package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuotaResponseDto(
    val totalQuota: Long = 10737418240L,
    val usedStorage: Long = 0L,
    val availableStorage: Long = 10737418240L,
    val percentageUsed: Double = 0.0,
    val fileCount: Int = 0
)

