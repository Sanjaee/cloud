package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 900L
)

@Serializable
data class GoogleTokenRequestDto(
    val idToken: String,
    val deviceId: String? = null
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)

@Serializable
data class AuthResponseDataDto(
    val user: UserDto? = null,
    val tokens: TokensDto? = null,
    val mfaRequired: Boolean = false,
    val mfaToken: String? = null,
    val email: String? = null
)

@Serializable
data class MfaVerifyRequestDto(
    val mfaToken: String,
    val code: String
)

