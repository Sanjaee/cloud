package com.cloudstorage.domain.model

data class FolderItem(
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

data class Breadcrumb(
    val id: String?,
    val name: String
)

