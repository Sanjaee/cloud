package com.cloudstorage.data.remote.api

import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.UserDto
import com.cloudstorage.data.remote.dto.UserProfileUpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApiService {
    @GET("users/me")
    suspend fun getCurrentUser(): Response<ApiResponseDto<UserDto>>

    @PATCH("users/profile")
    suspend fun updateProfile(
        @Body request: UserProfileUpdateRequestDto
    ): Response<ApiResponseDto<UserDto>>
}

