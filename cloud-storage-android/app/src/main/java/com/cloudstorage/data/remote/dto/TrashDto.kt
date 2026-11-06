package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrashResponseDto(
    val files: List<FileItemDto> = emptyList(),
    val folders: List<FolderItemDto> = emptyList()
)

@Serializable
data class TrashBatchActionRequestDto(
    val fileIds: List<String> = emptyList(),
    val folderIds: List<String> = emptyList()
)

