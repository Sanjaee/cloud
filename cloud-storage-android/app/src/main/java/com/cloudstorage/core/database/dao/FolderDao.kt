package com.cloudstorage.core.database.dao

import androidx.room.*
import com.cloudstorage.core.database.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE (:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId ORDER BY name ASC")
    fun getFoldersByParent(parentId: String?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isStarred = 1 ORDER BY name ASC")
    fun getStarredFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("DELETE FROM folders WHERE id IN (:ids)")
    suspend fun deleteFoldersByIds(ids: List<String>)

    @Query("DELETE FROM folders WHERE (:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId")
    suspend fun clearFoldersForParent(parentId: String?)

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}

