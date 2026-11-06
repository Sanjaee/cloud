package com.cloudstorage.feature.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.database.dao.FileDao
import com.cloudstorage.core.database.dao.FolderDao
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.FileRepository
import com.cloudstorage.domain.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StarredUiState(
    val files: List<FileItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class StarredViewModel @Inject constructor(
    fileDao: FileDao,
    folderDao: FolderDao,
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarredUiState())
    val uiState: StateFlow<StarredUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                fileDao.getStarredFiles().map { entities -> entities.map { it.toDomain() } },
                folderDao.getStarredFolders().map { entities -> entities.map { it.toDomain() } }
            ) { files, folders ->
                StarredUiState(files = files, folders = folders, isLoading = false)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun unstarFile(file: FileItem) {
        viewModelScope.launch {
            fileRepository.starFile(file.id, false)
        }
    }

    fun unstarFolder(folder: FolderItem) {
        viewModelScope.launch {
            folderRepository.starFolder(folder.id, false)
        }
    }
}

