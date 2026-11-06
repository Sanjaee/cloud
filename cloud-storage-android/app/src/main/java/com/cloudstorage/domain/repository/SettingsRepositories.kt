package com.cloudstorage.domain.repository

import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.NotificationItem
import com.cloudstorage.domain.model.Quota
import com.cloudstorage.domain.model.SessionItem

interface QuotaRepository {
    suspend fun getQuota(): NetworkResult<Quota>
}

interface NotificationRepository {
    suspend fun getNotifications(): NetworkResult<List<NotificationItem>>
    suspend fun markAsRead(id: String): NetworkResult<Unit>
    suspend fun markAllAsRead(): NetworkResult<Unit>
}

interface MfaRepository {
    suspend fun getMfaStatus(): NetworkResult<Boolean>
    suspend fun setupMfa(regenerate: Boolean = false): NetworkResult<Triple<String, String, String?>> // secret, qrCodeDataUrl, otpauthUrl
    suspend fun enableMfa(code: String): NetworkResult<List<String>> // backup codes
    suspend fun disableMfa(code: String?, password: String?): NetworkResult<Unit>
}

interface SessionRepository {
    suspend fun getSessions(): NetworkResult<List<SessionItem>>
    suspend fun revokeSession(id: String): NetworkResult<Unit>
    suspend fun revokeOtherSessions(): NetworkResult<Unit>
}

data class SecurityEvent(
    val id: String?,
    val eventType: String,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: String?
)

interface SecurityRepository {
    suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<Unit>
    suspend fun getSecurityEvents(): NetworkResult<List<SecurityEvent>>
}


