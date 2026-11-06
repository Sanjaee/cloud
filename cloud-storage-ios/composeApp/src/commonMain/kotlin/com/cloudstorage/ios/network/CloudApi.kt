package com.cloudstorage.ios.network

import com.cloudstorage.ios.model.*
import com.cloudstorage.ios.platform.PickedFile
import com.cloudstorage.ios.platform.PlatformBridge
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object CloudApi {
    const val BASE_URL = "https://cloud-storage-api-tau.vercel.app/api/v1"
    const val WEB_SHARE_BASE_URL = "https://cloud-storage-web-seven.vercel.app/share/"

    private const val KEY_ACCESS_TOKEN = "zacode_access_token"
    private const val KEY_REFRESH_TOKEN = "zacode_refresh_token"
    private const val KEY_USER_EMAIL = "zacode_user_email"
    private const val KEY_USERNAME = "zacode_username"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    fun getAccessToken(): String? = PlatformBridge.getString(KEY_ACCESS_TOKEN)
    fun getRefreshToken(): String? = PlatformBridge.getString(KEY_REFRESH_TOKEN)
    fun getStoredEmail(): String? = PlatformBridge.getString(KEY_USER_EMAIL)
    fun getStoredUsername(): String? = PlatformBridge.getString(KEY_USERNAME)
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun saveSession(tokens: TokensDto?, user: UserDto?) {
        tokens?.let {
            PlatformBridge.setString(KEY_ACCESS_TOKEN, it.accessToken)
            PlatformBridge.setString(KEY_REFRESH_TOKEN, it.refreshToken)
        }
        user?.let {
            PlatformBridge.setString(KEY_USER_EMAIL, it.email)
            PlatformBridge.setString(KEY_USERNAME, it.username ?: it.email.substringBefore("@"))
        }
    }

    fun clearSession() {
        PlatformBridge.clearStorage()
    }

    private fun HttpRequestBuilder.authHeader() {
        getAccessToken()?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        header(HttpHeaders.UserAgent, "ZacodeStorage-iOS/1.0.0")
    }

    // 1. Exchange Google OAuth Ticket for Mobile Bearer Tokens
    suspend fun exchangeGoogleTicket(ticket: String): Result<AuthResponseDataDto> = runCatching {
        val resp: ApiResponseDto<AuthResponseDataDto> = client.post("$BASE_URL/auth/google/exchange-ticket") {
            contentType(ContentType.Application.Json)
            setBody(ExchangeTicketRequestDto(ticket = ticket))
        }.body()
        if (!resp.success || resp.data == null) {
            throw Exception(resp.error?.message ?: resp.message ?: "Gagal menukar tiket login Google")
        }
        saveSession(resp.data.tokens, resp.data.user)
        resp.data
    }

    // 2. Email & Password Login (with optional MFA code)
    suspend fun loginWithCredentials(email: String, password: String, mfaCode: String? = null): Result<ApiResponseDto<AuthResponseDataDto>> = runCatching {
        val resp: ApiResponseDto<AuthResponseDataDto> = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email = email.trim(), password = password, mfaCode = mfaCode))
        }.body()
        if (!resp.success && !resp.mfaRequired) {
            throw Exception(resp.error?.message ?: resp.message ?: "Email atau password salah")
        }
        resp.data?.let { saveSession(it.tokens, it.user) }
        resp
    }

    // 3. Verify Google OAuth 2FA (TOTP / Backup Code)
    suspend fun verifyGoogleMfa(mfaToken: String, code: String): Result<AuthResponseDataDto> = runCatching {
        val resp: ApiResponseDto<AuthResponseDataDto> = client.post("$BASE_URL/auth/google/mfa-verify") {
            contentType(ContentType.Application.Json)
            setBody(MfaVerifyRequestDto(mfaToken = mfaToken, code = code.trim()))
        }.body()
        if (!resp.success || resp.data == null) {
            throw Exception(resp.error?.message ?: resp.message ?: "Kode 2FA tidak valid")
        }
        saveSession(resp.data.tokens, resp.data.user)
        resp.data
    }

    // 4. Fetch Drive Contents (Folders, Files, Breadcrumbs, Quota)
    suspend fun listFolders(parentId: String?): List<FolderItemDto> = runCatching {
        val resp: ApiResponseDto<FolderListResponseDto> = client.get("$BASE_URL/folders") {
            authHeader()
            if (parentId != null) parameter("parentId", parentId)
        }.body()
        resp.data?.folders ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun listFiles(folderId: String?): List<FileItemDto> = runCatching {
        val resp: ApiResponseDto<FileListResponseDto> = client.get("$BASE_URL/files") {
            authHeader()
            if (folderId != null) parameter("folderId", folderId)
        }.body()
        resp.data?.files ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun getBreadcrumbs(folderId: String): List<BreadcrumbDto> = runCatching {
        val resp: ApiResponseDto<BreadcrumbsResponseDto> = client.get("$BASE_URL/folders/$folderId/breadcrumbs") {
            authHeader()
        }.body()
        resp.data?.breadcrumbs ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun getQuota(): QuotaResponseDto = runCatching {
        val resp: ApiResponseDto<QuotaResponseDto> = client.get("$BASE_URL/quota") {
            authHeader()
        }.body()
        resp.data ?: QuotaResponseDto()
    }.getOrDefault(QuotaResponseDto())

    // 5. Folder & File Operations
    suspend fun createFolder(name: String, parentId: String?): Result<Unit> = runCatching {
        val resp: ApiResponseDto<JsonElement> = client.post("$BASE_URL/folders") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(FolderCreateRequestDto(name = name.trim(), parentId = parentId))
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal membuat folder")
    }

    suspend fun renameItem(id: String, isFolder: Boolean, newName: String): Result<Unit> = runCatching {
        val path = if (isFolder) "folders/$id/rename" else "files/$id/rename"
        val resp: ApiResponseDto<JsonElement> = client.patch("$BASE_URL/$path") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(RenameRequestDto(name = newName.trim()))
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal mengubah nama")
    }

    suspend fun deleteItem(id: String, isFolder: Boolean): Result<Unit> = runCatching {
        val path = if (isFolder) "folders/$id" else "files/$id"
        val resp: ApiResponseDto<JsonElement> = client.delete("$BASE_URL/$path") {
            authHeader()
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal memindahkan ke sampah")
    }

    suspend fun batchDelete(fileIds: List<String>, folderIds: List<String>): Result<Unit> = runCatching {
        val resp: ApiResponseDto<JsonElement> = client.post("$BASE_URL/files/batch-delete") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(BatchDeleteRequestDto(fileIds = fileIds, folderIds = folderIds))
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal menghapus item terpilih")
    }

    // 6. Upload File to Cloudflare R2
    suspend fun uploadFile(file: PickedFile, folderId: String?): Result<Unit> = runCatching {
        val initResp: ApiResponseDto<UploadInitiateResponseDto> = client.post("$BASE_URL/uploads/initiate") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(
                UploadInitiateRequestDto(
                    filename = file.name,
                    size = file.bytes.size.toLong(),
                    mimeType = file.mimeType,
                    folderId = folderId
                )
            )
        }.body()

        val initData = initResp.data ?: throw Exception(initResp.error?.message ?: "Gagal memulai upload")
        val uploadUrl = initData.uploadUrl ?: throw Exception("URL upload tidak ditemukan")

        val putResponse = client.put(uploadUrl) {
            header(HttpHeaders.ContentType, file.mimeType)
            setBody(file.bytes)
        }
        if (!putResponse.status.isSuccess()) {
            throw Exception("Gagal mengunggah file ke server penyimpanan (${putResponse.status.value})")
        }

        val completeResp: ApiResponseDto<JsonElement> = client.post("$BASE_URL/uploads/complete") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(UploadCompleteRequestDto(uploadSessionId = initData.uploadSessionId))
        }.body()

        if (!completeResp.success) {
            throw Exception(completeResp.error?.message ?: "Gagal menyelesaikan unggahan")
        }
    }

    // 7. File Preview & Download URL
    suspend fun getPreviewUrl(fileId: String): Result<String> = runCatching {
        val resp: ApiResponseDto<PreviewResponseDto> = client.get("$BASE_URL/files/$fileId/preview") {
            authHeader()
        }.body()
        resp.data?.previewUrl ?: getDownloadUrl(fileId).getOrThrow()
    }

    suspend fun getDownloadUrl(fileId: String): Result<String> = runCatching {
        val resp: ApiResponseDto<DownloadResponseDto> = client.get("$BASE_URL/files/$fileId/download") {
            authHeader()
        }.body()
        resp.data?.downloadUrl ?: throw Exception("URL unduhan tidak tersedia")
    }

    // 8. Sharing, Collaborators, Public Link & User Search
    suspend fun searchUsers(query: String): List<UserSearchItemDto> = runCatching {
        if (query.trim().length < 2) return emptyList()
        val resp: ApiResponseDto<UserSearchResponseDto> = client.get("$BASE_URL/shares/users/search") {
            authHeader()
            parameter("q", query.trim())
        }.body()
        resp.data?.users ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun shareResource(
        resourceType: String,
        resourceId: String,
        recipient: String,
        permission: String,
        message: String?
    ): Result<Unit> = runCatching {
        val resp: ApiResponseDto<JsonElement> = client.post("$BASE_URL/shares/user") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(
                ShareRequestDto(
                    resourceType = resourceType,
                    resourceId = resourceId,
                    recipientEmailOrUsername = recipient.trim(),
                    permission = permission,
                    message = message?.takeIf { it.isNotBlank() }
                )
            )
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal membagikan item")
    }

    suspend fun batchShare(
        fileIds: List<String>,
        folderIds: List<String>,
        recipient: String,
        permission: String,
        message: String?
    ): Result<Unit> = runCatching {
        val items = fileIds.map { ShareResourceItemDto("file", it) } +
            folderIds.map { ShareResourceItemDto("folder", it) }
        val resp: ApiResponseDto<JsonElement> = client.post("$BASE_URL/shares/user/batch") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(
                BatchShareRequestDto(
                    items = items,
                    fileIds = fileIds,
                    folderIds = folderIds,
                    recipientEmailOrUsername = recipient.trim(),
                    permission = permission,
                    message = message?.takeIf { it.isNotBlank() }
                )
            )
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal membagikan item terpilih")
    }

    suspend fun getCollaborators(resourceType: String, resourceId: String): List<CollaboratorDto> = runCatching {
        val resp: ApiResponseDto<CollaboratorListResponseDto> = client.get("$BASE_URL/shares/collaborators") {
            authHeader()
            parameter("resourceType", resourceType)
            parameter("resourceId", resourceId)
        }.body()
        resp.data?.collaborators ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun revokeCollaborator(permissionId: String): Result<Unit> = runCatching {
        client.delete("$BASE_URL/shares/user/$permissionId") {
            authHeader()
        }
    }

    suspend fun createPublicLink(resourceType: String, resourceId: String, permission: String): Result<String> = runCatching {
        val resp: ApiResponseDto<PublicLinkDataDto> = client.post("$BASE_URL/shares/links") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(CreatePublicLinkRequestDto(resourceType, resourceId, permission))
        }.body()
        val token = resp.data?.token ?: throw Exception(resp.error?.message ?: "Gagal membuat tautan publik")
        "$WEB_SHARE_BASE_URL$token"
    }

    suspend fun getSharedWithMe(): SharedItemsResponseDto = runCatching {
        val resp: ApiResponseDto<SharedItemsResponseDto> = client.get("$BASE_URL/shares/with-me") {
            authHeader()
        }.body()
        resp.data ?: SharedItemsResponseDto()
    }.getOrDefault(SharedItemsResponseDto())

    // 9. Trash Operations
    suspend fun getTrash(): TrashResponseDto = runCatching {
        val resp: ApiResponseDto<TrashResponseDto> = client.get("$BASE_URL/trash") {
            authHeader()
        }.body()
        resp.data ?: TrashResponseDto()
    }.getOrDefault(TrashResponseDto())

    suspend fun restoreFromTrash(id: String, isFolder: Boolean): Result<Unit> = runCatching {
        val body = if (isFolder) BatchDeleteRequestDto(folderIds = listOf(id)) else BatchDeleteRequestDto(fileIds = listOf(id))
        client.post("$BASE_URL/trash/restore") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun emptyTrash(): Result<Unit> = runCatching {
        client.delete("$BASE_URL/trash/empty") {
            authHeader()
        }
    }

    // 10. Profile, Security & TOTP 2FA Settings
    suspend fun getCurrentUser(): UserDto? = runCatching {
        val resp: ApiResponseDto<AuthResponseDataDto> = client.get("$BASE_URL/auth/me") {
            authHeader()
        }.body()
        resp.data?.user?.also { saveSession(null, it) }
    }.getOrNull()

    suspend fun getMfaStatus(): Boolean = runCatching {
        val resp: ApiResponseDto<MfaStatusDto> = client.get("$BASE_URL/mfa/status") {
            authHeader()
        }.body()
        resp.data?.isEnabled ?: false
    }.getOrDefault(false)

    suspend fun setupMfa(): Result<MfaSetupResponseDto> = runCatching {
        val resp: ApiResponseDto<MfaSetupResponseDto> = client.post("$BASE_URL/mfa/setup") {
            authHeader()
        }.body()
        resp.data ?: throw Exception(resp.error?.message ?: "Gagal menyiapkan 2FA")
    }

    suspend fun enableMfa(code: String): Result<List<String>> = runCatching {
        val resp: ApiResponseDto<MfaEnableResponseDto> = client.post("$BASE_URL/mfa/enable") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(MfaEnableRequestDto(code = code.trim(), totpCode = code.trim()))
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Kode TOTP tidak valid")
        resp.data?.backupCodes ?: emptyList()
    }

    suspend fun disableMfa(code: String): Result<Unit> = runCatching {
        val resp: ApiResponseDto<JsonElement> = client.post("$BASE_URL/mfa/disable") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(MfaDisableRequestDto(code = code.trim(), totpCode = code.trim()))
        }.body()
        if (!resp.success) throw Exception(resp.error?.message ?: "Gagal menonaktifkan 2FA")
    }

    suspend fun getSessions(): List<SessionItemDto> = runCatching {
        val resp: ApiResponseDto<SessionListResponseDto> = client.get("$BASE_URL/sessions") {
            authHeader()
        }.body()
        resp.data?.sessions ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun getSecurityEvents(): List<SecurityEventDto> = runCatching {
        val resp: ApiResponseDto<SecurityEventsResponseDto> = client.get("$BASE_URL/security/events") {
            authHeader()
        }.body()
        resp.data?.events ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun logout() {
        runCatching {
            client.post("$BASE_URL/auth/logout") {
                authHeader()
            }
        }
        clearSession()
    }
}

