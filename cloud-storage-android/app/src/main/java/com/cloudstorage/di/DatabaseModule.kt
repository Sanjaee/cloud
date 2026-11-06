package com.cloudstorage.di

import android.content.Context
import androidx.room.Room
import com.cloudstorage.core.database.AppDatabase
import com.cloudstorage.core.database.dao.FileDao
import com.cloudstorage.core.database.dao.FolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "zacode_cloud_storage.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFileDao(database: AppDatabase): FileDao = database.fileDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()
}

