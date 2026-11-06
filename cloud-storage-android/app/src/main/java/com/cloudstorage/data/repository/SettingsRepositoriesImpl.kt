package com.cloudstorage.data.repository

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.MfaApiService
import com.cloudstorage.data.remote.api.NotificationApiService
import com.cloudstorage.data.remote.api.QuotaApiService
import com.cloudstorage.data.remote.api.SessionApiService
import com.cloudstorage.data.remote.dto.MfaDisableRequestDto
import com.cloudstorage.data.remote.dto.MfaEnableRequestDto
import com.cloudstorage.data.remote.dto.MfaSetupRequestDto
import com.cloudstorage.domain.model.NotificationItem
import com.cloudstorage.domain.model.Quota
import com.cloudstorage.domain.model.SessionItem
import com.cloudstorage.domain.repository.MfaRepository
import com.cloudstorage.domain.repository.NotificationRepository
import com.cloudstorage.domain.repository.QuotaRepository
import com.cloudstorage.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotaRepositoryImpl @Inject constructor(
    private val quotaApiService: QuotaApiService,
    private val apiErrorHandler: ApiErrorHandler
) : QuotaRepository {
    override suspend fun getQuota(): NetworkResult<Quota> {
        return try {
            val response = quotaApiService.getQuota()
            if (response.isSuccessful && response.body()?.data != null) {
                val d = response.body()!!.data!!
                NetworkResult.Success(
                    Quota(
                        totalQuota = d.totalQuota,
                        usedStorage = d.usedStorage,
                        availableStorage = d.availableStorage,
                        percentageUsed = d.percentageUsed,
                        fileCount = d.fileCount
                    )
                )
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load quota")
        }
    }
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationApiService: NotificationApiService,
    private val apiErrorHandler: ApiErrorHandler
) : NotificationRepository {
    override suspend fun getNotifications(): NetworkResult<List<NotificationItem>> {
        return try {
            val response = notificationApiService.getNotifications()
            if (response.isSuccessful && response.body()?.data != null) {
                val dtos = response.body()!!.data!!.notifications
                val list = dtos.map { dto ->
                    NotificationItem(
                        id = dto.id,
                        userId = dto.userId,
                        actorId = dto.actorId,
                        type = dto.type,
                        resourceType = dto.resourceType,
                        resourceId = dto.resourceId,
                        message = dto.message,
                        isRead = dto.isRead,
                        createdAt = dto.createdAt
                    )
                }
                NetworkResult.Success(list)
            } else {
                val msg = apiErrorHandler.parseErrorMessage(response)
                NetworkResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load notifications")
        }
    }

    override suspend fun markAsRead(id: String): NetworkResult<Unit> {
        return try {
            val response = notificationApiService.markAsRead(id)
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to mark as read")
        }
    }

    override suspend fun markAllAsRead(): NetworkResult<Unit> {
        return try {
            val response = notificationApiService.markAllAsRead()
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to mark all as read")
        }
    }
}

@Singleton
class MfaRepositoryImpl @Inject constructor(
    private val mfaApiService: MfaApiService,
    private val apiErrorHandler: ApiErrorHandler
) : MfaRepository {
    override suspend fun getMfaStatus(): NetworkResult<Boolean> {
        return try {
            val response = mfaApiService.getMfaStatus()
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!.isEnabled)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load 2FA status")
        }
    }

    override suspend fun setupMfa(regenerate: Boolean): NetworkResult<Triple<String, String, String?>> {
        return try {
            val response = mfaApiService.setupMfa(MfaSetupRequestDto(regenerate = regenerate))
            if (response.isSuccessful && response.body()?.data != null) {
                val d = response.body()!!.data!!
                NetworkResult.Success(Triple(d.secret, d.qrCodeDataUrl, d.otpauthUrl))
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to initiate 2FA setup")
        }
    }

    override suspend fun enableMfa(code: String): NetworkResult<List<String>> {
        return try {
            val response = mfaApiService.enableMfa(MfaEnableRequestDto(code = code, totpCode = code))
            if (response.isSuccessful && response.body()?.data != null) {
                NetworkResult.Success(response.body()!!.data!!.backupCodes)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to enable 2FA")
        }
    }

    override suspend fun disableMfa(code: String?, password: String?): NetworkResult<Unit> {
        return try {
            val response = mfaApiService.disableMfa(
                MfaDisableRequestDto(code = code, totpCode = code, password = password)
            )
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to disable 2FA")
        }
    }
}

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionApiService: SessionApiService,
    private val apiErrorHandler: ApiErrorHandler
) : SessionRepository {
    override suspend fun getSessions(): NetworkResult<List<SessionItem>> {
        return try {
            val response = sessionApiService.getSessions()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.sessions.map { dto ->
                    SessionItem(
                        id = dto.id,
                        userId = dto.userId,
                        userAgent = dto.userAgent,
                        ipAddress = dto.ipAddress,
                        deviceType = dto.deviceType,
                        isCurrent = dto.isCurrent,
                        createdAt = dto.createdAt,
                        lastActiveAt = dto.lastUsedAt ?: dto.lastActiveAt
                    )
                }
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load sessions")
        }
    }

    override suspend fun revokeSession(id: String): NetworkResult<Unit> {
        return try {
            val response = sessionApiService.revokeSession(id)
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to revoke session")
        }
    }

    override suspend fun revokeOtherSessions(): NetworkResult<Unit> {
        return try {
            val response = sessionApiService.revokeOtherSessions()
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to revoke other sessions")
        }
    }
}

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val securityApiService: com.cloudstorage.data.remote.api.SecurityApiService,
    private val apiErrorHandler: ApiErrorHandler
) : com.cloudstorage.domain.repository.SecurityRepository {
    override suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<Unit> {
        return try {
            val response = securityApiService.changePassword(
                com.cloudstorage.data.remote.dto.ChangePasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) NetworkResult.Success(Unit)
            else NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to change password")
        }
    }

    override suspend fun getSecurityEvents(): NetworkResult<List<com.cloudstorage.domain.repository.SecurityEvent>> {
        return try {
            val response = securityApiService.getSecurityEvents()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.events.map { dto ->
                    com.cloudstorage.domain.repository.SecurityEvent(
                        id = dto.id,
                        eventType = dto.eventType,
                        ipAddress = dto.ipAddress,
                        userAgent = dto.userAgent,
                        createdAt = dto.createdAt
                    )
                }
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(apiErrorHandler.parseErrorMessage(response), response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to load security events")
        }
    }
}


