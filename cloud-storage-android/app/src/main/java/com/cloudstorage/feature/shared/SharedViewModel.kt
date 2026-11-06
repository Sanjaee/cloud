package com.cloudstorage.feature.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.ShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedUiState(
    val withMeFiles: List<FileItem> = emptyList(),
    val withMeFolders: List<FolderItem> = emptyList(),
    val byMeFiles: List<FileItem> = emptyList(),
    val byMeFolders: List<FolderItem> = emptyList(),
    val selectedTab: Int = 0, // 0 = with me, 1 = by me
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val shareRepository: ShareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedUiState())
    val uiState: StateFlow<SharedUiState> = _uiState.asStateFlow()

    init {
        loadShared()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun loadShared() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val withMeRes = shareRepository.getSharedWithMe()
            val byMeRes = shareRepository.getSharedByMe()

            _uiState.update { state ->
                state.copy(
                    withMeFiles = if (withMeRes is NetworkResult.Success) withMeRes.data.first else emptyList(),
                    withMeFolders = if (withMeRes is NetworkResult.Success) withMeRes.data.second else emptyList(),
                    byMeFiles = if (byMeRes is NetworkResult.Success) byMeRes.data.first else emptyList(),
                    byMeFolders = if (byMeRes is NetworkResult.Success) byMeRes.data.second else emptyList(),
                    isLoading = false
                )
            }
        }
    }
}

