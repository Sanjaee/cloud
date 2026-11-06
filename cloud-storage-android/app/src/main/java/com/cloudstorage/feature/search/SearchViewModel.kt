package com.cloudstorage.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { q ->
                    if (q.trim().length >= 2) {
                        performSearch(q.trim())
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    private suspend fun performSearch(q: String) {
        _isSearching.value = true
        when (val res = fileRepository.searchFiles(q)) {
            is NetworkResult.Success -> _searchResults.value = res.data
            else -> _searchResults.value = emptyList()
        }
        _isSearching.value = false
    }
}

