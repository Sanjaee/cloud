package com.cloudstorage.feature.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecentFiles()
    }

    fun loadRecentFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val res = fileRepository.refreshFiles(null)) {
                is NetworkResult.Success -> {
                    // Sort descending by createdAt/updatedAt
                    val sorted = res.data.sortedByDescending { it.updatedAt ?: it.createdAt }
                    _recentFiles.value = sorted
                }
                else -> Unit
            }
            _isLoading.value = false
        }
    }
}

