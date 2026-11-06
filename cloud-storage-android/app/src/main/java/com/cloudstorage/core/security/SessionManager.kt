package com.cloudstorage.core.security

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) {
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    fun notifySessionExpired() {
        tokenStorage.clear()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    fun logout() {
        tokenStorage.clear()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    fun isAuthenticated(): Boolean = tokenStorage.hasValidSession()
}

