package com.cloudstorage.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cloudstorage.domain.model.FileItem

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["isStarred"]),
        Index(value = ["name"])
    ]
)
data class FileEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String?,
    val folderId: String?,
    val storageObjectId: String?,
    val name: String,
    val originalName: String?,
    val mimeType: String,
    val size: Long,
    val isStarred: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
    val deletedAt: String?
) {
    fun toDomain(): FileItem = FileItem(
        id = id,
        ownerId = ownerId,
        folderId = folderId,
        storageObjectId = storageObjectId,
        name = name,
        originalName = originalName,
        mimeType = mimeType,
        size = size,
        isStarred = isStarred,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    companion object {
        fun fromDomain(file: FileItem): FileEntity = FileEntity(
            id = file.id,
            ownerId = file.ownerId,
            folderId = file.folderId,
            storageObjectId = file.storageObjectId,
            name = file.name,
            originalName = file.originalName,
            mimeType = file.mimeType,
            size = file.size,
            isStarred = file.isStarred,
            createdAt = file.createdAt,
            updatedAt = file.updatedAt,
            deletedAt = file.deletedAt
        )
    }
}

