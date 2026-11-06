package com.cloudstorage.core.database.dao

import androidx.room.*
import com.cloudstorage.core.database.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId ORDER BY name ASC")
    fun getFilesByFolder(folderId: String?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isStarred = 1 ORDER BY name ASC")
    fun getStarredFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: String)

    @Query("DELETE FROM files WHERE id IN (:ids)")
    suspend fun deleteFilesByIds(ids: List<String>)

    @Query("DELETE FROM files WHERE (:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId")
    suspend fun clearFilesForFolder(folderId: String?)

    @Query("DELETE FROM files")
    suspend fun clearAll()
}

