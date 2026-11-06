package com.cloudstorage.feature.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.domain.model.Quota
import com.cloudstorage.domain.repository.QuotaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val quotaRepository: QuotaRepository
) : ViewModel() {

    private val _quota = MutableStateFlow<Quota?>(null)
    val quota: StateFlow<Quota?> = _quota.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadQuota()
    }

    fun loadQuota() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val res = quotaRepository.getQuota()) {
                is NetworkResult.Success -> _quota.value = res.data
                else -> Unit
            }
            _isLoading.value = false
        }
    }
}

