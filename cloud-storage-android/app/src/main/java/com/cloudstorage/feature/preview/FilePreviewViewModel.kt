package com.cloudstorage.feature.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.repository.DownloadRepository
import com.cloudstorage.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val file: FileItem? = null,
    val previewUrl: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class FilePreviewViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val downloadRepository: DownloadRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val fileId: String = savedStateHandle["fileId"] ?: ""

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    init {
        loadFileInfo()
    }

    private fun loadFileInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val fileRes = fileRepository.getFileDetail(fileId)
            val urlRes = downloadRepository.getPreviewUrl(fileId)

            _uiState.update { state ->
                state.copy(
                    file = if (fileRes is NetworkResult.Success) fileRes.data else null,
                    previewUrl = if (urlRes is NetworkResult.Success) urlRes.data else null,
                    isLoading = false,
                    errorMessage = if (fileRes is NetworkResult.Error) fileRes.message else null
                )
            }
        }
    }

    fun downloadFile() {
        val f = _uiState.value.file ?: return
        downloadRepository.enqueueDownload(f.id, f.name)
    }

    fun toggleStar() {
        val f = _uiState.value.file ?: return
        viewModelScope.launch {
            val newStarred = !f.isStarred
            fileRepository.starFile(f.id, newStarred)
            _uiState.update { state ->
                state.copy(file = state.file?.copy(isStarred = newStarred))
            }
        }
    }
}

