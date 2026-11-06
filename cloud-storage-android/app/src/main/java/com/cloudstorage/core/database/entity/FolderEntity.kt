package com.cloudstorage.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cloudstorage.domain.model.FolderItem

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["isStarred"]),
        Index(value = ["name"])
    ]
)
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String?,
    val parentId: String?,
    val name: String,
    val color: String?,
    val isStarred: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
    val deletedAt: String?
) {
    fun toDomain(): FolderItem = FolderItem(
        id = id,
        ownerId = ownerId,
        parentId = parentId,
        name = name,
        color = color,
        isStarred = isStarred,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    companion object {
        fun fromDomain(folder: FolderItem): FolderEntity = FolderEntity(
            id = folder.id,
            ownerId = folder.ownerId,
            parentId = folder.parentId,
            name = folder.name,
            color = folder.color,
            isStarred = folder.isStarred,
            createdAt = folder.createdAt,
            updatedAt = folder.updatedAt,
            deletedAt = folder.deletedAt
        )
    }
}

