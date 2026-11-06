package com.cloudstorage.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackendErrorResponse(
    val success: Boolean? = false,
    val error: BackendErrorDetail? = null,
    val message: String? = null
)

@Serializable
data class BackendErrorDetail(
    val code: String? = null,
    val message: String? = null
)

@Singleton
class ApiErrorHandler @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            val (_, message) = parseErrorBody(errorBody)
            message ?: response.message().takeIf { it.isNotBlank() } ?: "HTTP ${response.code()} error"
        } catch (_: Exception) {
            "Request failed with status ${response.code()}"
        }
    }

    fun handleError(throwable: Throwable): NetworkResult.Error {
        return when (throwable) {
            is HttpException -> {
                val statusCode = throwable.code()
                val errorBody = throwable.response()?.errorBody()?.string()
                val (code, message) = parseErrorBody(errorBody)
                NetworkResult.Error(
                    message = message ?: throwable.message ?: "HTTP $statusCode error",
                    code = code,
                    statusCode = statusCode
                )
            }
            is IOException -> {
                NetworkResult.Error(
                    message = "Kesalahan koneksi jaringan. Periksa koneksi internet Anda.",
                    code = "NETWORK_UNAVAILABLE"
                )
            }
            else -> {
                NetworkResult.Error(
                    message = throwable.localizedMessage ?: "Terjadi kesalahan sistem yang tidak terduga",
                    code = "UNKNOWN_ERROR"
                )
            }
        }
    }

    private fun parseErrorBody(errorBody: String?): Pair<String?, String?> {
        if (errorBody.isNullOrBlank()) return Pair(null, null)
        return try {
            val parsed = json.decodeFromString<BackendErrorResponse>(errorBody)
            val code = parsed.error?.code
            val message = parsed.error?.message ?: parsed.message
            Pair(code, message)
        } catch (_: Exception) {
            Pair(null, errorBody)
        }
    }
}
