package com.cloudstorage.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FolderItemDto(
    val id: String,
    val ownerId: String? = null,
    val parentId: String? = null,
    val name: String,
    val color: String? = null,
    val isStarred: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
)

@Serializable
data class FolderListResponseDto(
    val folders: List<FolderItemDto> = emptyList()
)

@Serializable
data class FolderCreateRequestDto(
    val name: String,
    val parentId: String? = null,
    val color: String? = null
)

@Serializable
data class FolderRenameRequestDto(
    val name: String
)

@Serializable
data class FolderMoveRequestDto(
    val targetParentId: String?
)

@Serializable
data class FolderStarRequestDto(
    val isStarred: Boolean
)

@Serializable
data class BreadcrumbDto(
    val id: String? = null,
    val name: String
)

@Serializable
data class BreadcrumbsResponseDto(
    val breadcrumbs: List<BreadcrumbDto> = emptyList()
)

