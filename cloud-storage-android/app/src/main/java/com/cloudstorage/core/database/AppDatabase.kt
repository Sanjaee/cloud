package com.cloudstorage.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cloudstorage.core.database.dao.FileDao
import com.cloudstorage.core.database.dao.FolderDao
import com.cloudstorage.core.database.entity.FileEntity
import com.cloudstorage.core.database.entity.FolderEntity

@Database(
    entities = [
        FileEntity::class,
        FolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun folderDao(): FolderDao
}

