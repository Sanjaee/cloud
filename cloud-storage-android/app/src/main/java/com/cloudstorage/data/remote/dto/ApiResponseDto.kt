package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: ApiErrorDto? = null
)

@Serializable
data class ApiErrorDto(
    val code: String = "ERROR",
    val message: String = "An error occurred",
    val details: JsonElement? = null
)

