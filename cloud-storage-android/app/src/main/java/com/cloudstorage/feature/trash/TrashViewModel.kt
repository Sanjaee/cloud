package com.cloudstorage.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val files: List<FileItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFileIds: Set<String> = emptySet(),
    val selectedFolderIds: Set<String> = emptySet()
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrash()
    }

    fun loadTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = trashRepository.getTrash()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            files = res.data.first,
                            folders = res.data.second,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun restoreSingleItem(type: String, id: String) {
        viewModelScope.launch {
            trashRepository.restoreItem(type, id)
            loadTrash()
        }
    }

    fun deleteSingleItemPermanently(type: String, id: String) {
        viewModelScope.launch {
            trashRepository.deletePermanently(type, id)
            loadTrash()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashRepository.emptyTrash()
            loadTrash()
        }
    }

    fun toggleFileSelection(id: String) {
        _uiState.update { state ->
            val set = state.selectedFileIds.toMutableSet()
            if (set.contains(id)) set.remove(id) else set.add(id)
            state.copy(selectedFileIds = set)
        }
    }

    fun toggleFolderSelection(id: String) {
        _uiState.update { state ->
            val set = state.selectedFolderIds.toMutableSet()
            if (set.contains(id)) set.remove(id) else set.add(id)
            state.copy(selectedFolderIds = set)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                selectedFileIds = state.files.map { it.id }.toSet(),
                selectedFolderIds = state.folders.map { it.id }.toSet()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFileIds = emptySet(), selectedFolderIds = emptySet()) }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val fileIds = _uiState.value.selectedFileIds.toList()
            val folderIds = _uiState.value.selectedFolderIds.toList()
            trashRepository.restoreBatch(fileIds, folderIds)
            clearSelection()
            loadTrash()
        }
    }

    fun deletePermanentlySelected() {
        viewModelScope.launch {
            val fileIds = _uiState.value.selectedFileIds.toList()
            val folderIds = _uiState.value.selectedFolderIds.toList()
            trashRepository.deletePermanentlyBatch(fileIds, folderIds)
            clearSelection()
            loadTrash()
        }
    }
}

