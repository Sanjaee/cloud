package com.cloudstorage.di

import com.cloudstorage.core.network.AuthInterceptor
import com.cloudstorage.core.network.TokenAuthenticator
import com.cloudstorage.data.remote.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://cloud-storage-api-tau.vercel.app/api/v1/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideFileApiService(retrofit: Retrofit): FileApiService = retrofit.create(FileApiService::class.java)

    @Provides
    @Singleton
    fun provideFolderApiService(retrofit: Retrofit): FolderApiService = retrofit.create(FolderApiService::class.java)

    @Provides
    @Singleton
    fun provideUploadApiService(retrofit: Retrofit): UploadApiService = retrofit.create(UploadApiService::class.java)

    @Provides
    @Singleton
    fun provideDownloadApiService(retrofit: Retrofit): DownloadApiService = retrofit.create(DownloadApiService::class.java)

    @Provides
    @Singleton
    fun provideShareApiService(retrofit: Retrofit): ShareApiService = retrofit.create(ShareApiService::class.java)

    @Provides
    @Singleton
    fun provideTrashApiService(retrofit: Retrofit): TrashApiService = retrofit.create(TrashApiService::class.java)

    @Provides
    @Singleton
    fun provideQuotaApiService(retrofit: Retrofit): QuotaApiService = retrofit.create(QuotaApiService::class.java)

    @Provides
    @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService = retrofit.create(NotificationApiService::class.java)

    @Provides
    @Singleton
    fun provideMfaApiService(retrofit: Retrofit): MfaApiService = retrofit.create(MfaApiService::class.java)

    @Provides
    @Singleton
    fun provideSessionApiService(retrofit: Retrofit): SessionApiService = retrofit.create(SessionApiService::class.java)

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService = retrofit.create(UserApiService::class.java)

    @Provides
    @Singleton
    fun provideSecurityApiService(retrofit: Retrofit): SecurityApiService = retrofit.create(SecurityApiService::class.java)
}

