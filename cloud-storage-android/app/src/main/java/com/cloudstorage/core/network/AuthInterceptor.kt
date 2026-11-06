package com.cloudstorage.core.network

import com.cloudstorage.core.security.SecureTokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        // If request is going to external storage (e.g. Cloudflare R2 presigned URL, S3, etc.),
        // NEVER attach API Authorization header or custom headers because R2 will reject with 400 InvalidArgument
        val isBackendApi = host.contains("cloud-storage-api-tau.vercel.app") || host.contains("localhost")
        if (!isBackendApi) {
            return chain.proceed(original)
        }

        // Skip auth header for Google direct login / refresh endpoints if needed
        val path = original.url.encodedPath
        val isPublicAuthPath = path.contains("/auth/google/token") || path.contains("/auth/refresh")

        val builder = original.newBuilder()
            .header("User-Agent", "ZacodeStorage-Android/1.0.0")
            .header("Accept", "application/json")

        if (!isPublicAuthPath) {
            val token = tokenStorage.getAccessToken()
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(builder.build())
    }
}

