package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileItemDto(
    val id: String,
    val ownerId: String? = null,
    val folderId: String? = null,
    val storageObjectId: String? = null,
    val name: String,
    val originalName: String? = null,
    val mimeType: String = "application/octet-stream",
    val size: Long = 0L,
    val isStarred: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
)

@Serializable
data class FileListResponseDto(
    val files: List<FileItemDto> = emptyList()
)

@Serializable
data class FileDetailResponseDto(
    val file: FileItemDto
)

@Serializable
data class FileRenameRequestDto(
    val name: String
)

@Serializable
data class FileMoveRequestDto(
    val targetFolderId: String?
)

@Serializable
data class FileStarRequestDto(
    val isStarred: Boolean
)

@Serializable
data class BatchDeleteRequestDto(
    val fileIds: List<String> = emptyList(),
    val folderIds: List<String> = emptyList()
)

