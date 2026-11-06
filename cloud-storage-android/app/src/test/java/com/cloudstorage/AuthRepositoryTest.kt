package com.cloudstorage

import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.security.SecureTokenStorage
import com.cloudstorage.core.security.SessionManager
import com.cloudstorage.data.remote.api.AuthApiService
import com.cloudstorage.data.remote.api.UserApiService
import com.cloudstorage.data.remote.dto.*
import com.cloudstorage.data.repository.AuthRepositoryImpl
import com.cloudstorage.domain.repository.GoogleAuthResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private val authApiService: AuthApiService = mockk()
    private val userApiService: UserApiService = mockk()
    private val secureTokenStorage: SecureTokenStorage = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val apiErrorHandler: ApiErrorHandler = mockk(relaxed = true)

    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        authRepository = AuthRepositoryImpl(
            authApiService = authApiService,
            userApiService = userApiService,
            secureTokenStorage = secureTokenStorage,
            sessionManager = sessionManager,
            apiErrorHandler = apiErrorHandler
        )
    }

    @Test
    fun `signInWithGoogleIdToken returns Success when backend returns valid tokens and user`() = runTest {
        val fakeUser = UserDto(
            id = "user_123",
            email = "user@zacode.com",
            username = "zacode_user"
        )
        val fakeTokens = TokensDto(
            accessToken = "access_token_123",
            refreshToken = "refresh_token_123"
        )
        val apiResponse = ApiResponseDto(
            success = true,
            data = AuthResponseDataDto(
                user = fakeUser,
                tokens = fakeTokens,
                mfaRequired = false
            )
        )

        coEvery { authApiService.signInWithGoogle(any()) } returns Response.success(apiResponse)

        val result = authRepository.signInWithGoogleIdToken("valid_google_id_token")

        assertTrue(result is GoogleAuthResult.Success)
        val success = result as GoogleAuthResult.Success
        assertEquals("user_123", success.user.id)
        assertEquals("user@zacode.com", success.user.email)

        verify { secureTokenStorage.saveAccessToken("access_token_123") }
        verify { secureTokenStorage.saveRefreshToken("refresh_token_123") }
    }

    @Test
    fun `signInWithGoogleIdToken returns MfaRequired when user has 2FA enabled`() = runTest {
        val apiResponse = ApiResponseDto(
            success = true,
            data = AuthResponseDataDto(
                mfaRequired = true,
                mfaToken = "mfa_pending_token_abc",
                email = "user@zacode.com"
            )
        )

        coEvery { authApiService.signInWithGoogle(any()) } returns Response.success(apiResponse)

        val result = authRepository.signInWithGoogleIdToken("valid_google_id_token")

        assertTrue(result is GoogleAuthResult.MfaRequired)
        val mfaReq = result as GoogleAuthResult.MfaRequired
        assertEquals("mfa_pending_token_abc", mfaReq.mfaToken)
        assertEquals("user@zacode.com", mfaReq.email)
    }

    @Test
    fun `isAuthenticated delegates to secureTokenStorage hasValidSession`() {
        every { secureTokenStorage.hasValidSession() } returns true
        assertTrue(authRepository.isAuthenticated())

        every { secureTokenStorage.hasValidSession() } returns false
        org.junit.Assert.assertFalse(authRepository.isAuthenticated())
    }
}

