package com.cloudstorage

import com.cloudstorage.core.database.dao.FileDao
import com.cloudstorage.core.network.ApiErrorHandler
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.data.remote.api.FileApiService
import com.cloudstorage.data.remote.dto.ApiResponseDto
import com.cloudstorage.data.remote.dto.FileItemDto
import com.cloudstorage.data.remote.dto.FileListResponseDto
import com.cloudstorage.data.repository.FileRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FileRepositoryTest {

    private val fileApiService: FileApiService = mockk()
    private val fileDao: FileDao = mockk(relaxed = true)
    private val apiErrorHandler: ApiErrorHandler = mockk(relaxed = true)

    private lateinit var fileRepository: FileRepositoryImpl

    @Before
    fun setup() {
        fileRepository = FileRepositoryImpl(
            fileApiService = fileApiService,
            fileDao = fileDao,
            apiErrorHandler = apiErrorHandler
        )
    }

    @Test
    fun `refreshFiles fetches from API and caches into Room database`() = runTest {
        val fakeFiles = listOf(
            FileItemDto(
                id = "file_1",
                name = "report.pdf",
                mimeType = "application/pdf",
                size = 1048576L
            )
        )
        val apiResponse = ApiResponseDto(
            success = true,
            data = FileListResponseDto(files = fakeFiles)
        )

        coEvery { fileApiService.getFiles(null) } returns Response.success(apiResponse)

        val result = fileRepository.refreshFiles(null)

        assertTrue(result is NetworkResult.Success)
        val success = result as NetworkResult.Success
        assertEquals(1, success.data.size)
        assertEquals("report.pdf", success.data[0].name)

        coVerify { fileDao.clearFilesForFolder(null) }
        coVerify { fileDao.insertFiles(any()) }
    }

    @Test
    fun `deleteFile calls API and deletes from local database`() = runTest {
        coEvery { fileApiService.deleteFile("file_1") } returns Response.success(
            ApiResponseDto(success = true)
        )

        val result = fileRepository.deleteFile("file_1")

        assertTrue(result is NetworkResult.Success)
        coVerify { fileDao.deleteFileById("file_1") }
    }
}

