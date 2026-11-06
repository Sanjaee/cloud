package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MfaStatusDto(
    val isEnabled: Boolean = false
)

@Serializable
data class MfaSetupResponseDto(
    val secret: String,
    val qrCodeDataUrl: String,
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
    val code: String? = null,
    val totpCode: String? = null,
    val password: String? = null
)

