package com.cloudstorage.feature.drive

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudstorage.core.network.NetworkResult
import com.cloudstorage.core.security.SecureTokenStorage
import com.cloudstorage.domain.model.Breadcrumb
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.domain.repository.DownloadRepository
import com.cloudstorage.domain.repository.FileRepository
import com.cloudstorage.domain.repository.FolderRepository
import com.cloudstorage.domain.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DriveUiState(
    val currentFolderId: String? = null,
    val breadcrumbs: List<Breadcrumb> = listOf(Breadcrumb(id = null, name = "My Drive")),
    val folders: List<FolderItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadingFileName: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val selectedFileIds: Set<String> = emptySet(),
    val selectedFolderIds: Set<String> = emptySet()
)

@HiltViewModel
class DriveViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val uploadRepository: UploadRepository,
    private val downloadRepository: DownloadRepository,
    private val secureTokenStorage: SecureTokenStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialFolderId: String? = savedStateHandle["folderId"]

    private val _currentFolderId = MutableStateFlow<String?>(initialFolderId)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    private val _uiState = MutableStateFlow(DriveUiState())
    val uiState: StateFlow<DriveUiState> = _uiState.asStateFlow()

    val accessToken: String? get() = secureTokenStorage.getAccessToken()

    private var currentLoadJob: Job? = null

    init {
        viewModelScope.launch {
            _currentFolderId.collectLatest { folderId ->
                loadDriveContent(folderId)
            }
        }
    }

    fun openFolder(folderId: String?) {
        _currentFolderId.value = folderId
        clearSelection()
    }

    fun navigateUp() {
        val crumbs = _uiState.value.breadcrumbs
        if (crumbs.size > 2) {
            openFolder(crumbs[crumbs.size - 2].id)
        } else {
            openFolder(null)
        }
    }

    fun refresh() {
        loadDriveContent(_currentFolderId.value)
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            folderRepository.refreshFolders(_currentFolderId.value)
            val fileRes = fileRepository.refreshFiles(_currentFolderId.value)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    errorMessage = if (fileRes is NetworkResult.Error) fileRes.message else null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun loadDriveContent(folderId: String?) {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            val hasCachedData = _uiState.value.folders.isNotEmpty() || _uiState.value.files.isNotEmpty()
            _uiState.update {
                it.copy(
                    currentFolderId = folderId,
                    isLoading = !hasCachedData,
                    errorMessage = null
                )
            }

            // Immediately observe local Room cache on IO Dispatcher for 0ms latency
            launch(Dispatchers.IO) {
                folderRepository.getFoldersFlow(folderId).collect { folders ->
                    _uiState.update { it.copy(folders = folders) }
                }
            }

            launch(Dispatchers.IO) {
                fileRepository.getFilesFlow(folderId).collect { files ->
                    _uiState.update { it.copy(files = files) }
                }
            }

            // Fetch breadcrumbs
            if (folderId != null) {
                when (val bcRes = folderRepository.getBreadcrumbs(folderId)) {
                    is NetworkResult.Success -> {
                        val crumbs = if (bcRes.data.isNotEmpty() && bcRes.data.first().id != null) {
                            listOf(Breadcrumb(id = null, name = "My Drive")) + bcRes.data
                        } else {
                            bcRes.data
                        }
                        _uiState.update { it.copy(breadcrumbs = crumbs) }
                    }
                    else -> Unit
                }
            } else {
                _uiState.update { it.copy(breadcrumbs = listOf(Breadcrumb(id = null, name = "My Drive"))) }
            }

            // Fetch fresh data from network in background (stale-while-revalidate)
            folderRepository.refreshFolders(folderId)
            val fileRes = fileRepository.refreshFiles(folderId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = if (fileRes is NetworkResult.Error) fileRes.message else null
                )
            }
        }
    }

    fun uploadFiles(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            val total = uris.size

            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0,
                    uploadingFileName = "Menyiapkan $total file...",
                    errorMessage = null,
                    successMessage = null
                )
            }

            for ((index, uri) in uris.withIndex()) {
                try {
                    var fileName = "file_${System.currentTimeMillis()}_${index + 1}"
                    var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    var fileSize = 0L

                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }

                    val currentProgress = ((index.toFloat() / total) * 100).toInt()
                    _uiState.update {
                        it.copy(
                            uploadProgress = currentProgress,
                            uploadingFileName = "(${(index + 1)}/$total) $fileName"
                        )
                    }

                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }

                    if (bytes == null || bytes.isEmpty()) {
                        failCount++
                        continue
                    }

                    if (fileSize <= 0L) {
                        fileSize = bytes.size.toLong()
                    }

                    val result = withContext(Dispatchers.IO) {
                        uploadRepository.directUpload(
                            name = fileName,
                            mimeType = mimeType,
                            size = fileSize,
                            bytes = bytes,
                            folderId = _currentFolderId.value
                        )
                    }

                    if (result is NetworkResult.Success) {
                        successCount++
                    } else {
                        failCount++
                    }
                } catch (_: Exception) {
                    failCount++
                }
            }

            _uiState.update {
                it.copy(
                    isUploading = false,
                    uploadProgress = 100,
                    uploadingFileName = null,
                    successMessage = if (successCount > 0) "$successCount file berhasil diunggah!" else null,
                    errorMessage = if (failCount > 0) "$failCount file gagal diunggah." else null
                )
            }

            if (successCount > 0) {
                refresh()
            }
        }
    }

    fun uploadFile(uri: Uri, context: Context) {
        uploadFiles(listOf(uri), context)
    }

    fun createFolder(name: String, color: String? = null) {
        viewModelScope.launch {
            when (val res = folderRepository.createFolder(name, _currentFolderId.value, color)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(successMessage = "Folder \"$name\" berhasil dibuat!") }
                    refresh()
                }
                is NetworkResult.Error -> _uiState.update { it.copy(errorMessage = res.message) }
                else -> Unit
            }
        }
    }

    fun renameFile(fileId: String, newName: String) {
        viewModelScope.launch {
            fileRepository.renameFile(fileId, newName)
            refresh()
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            folderRepository.renameFolder(folderId, newName)
            refresh()
        }
    }

    fun toggleStarFile(file: FileItem) {
        viewModelScope.launch {
            fileRepository.starFile(file.id, !file.isStarred)
            refresh()
        }
    }

    fun toggleStarFolder(folder: FolderItem) {
        viewModelScope.launch {
            folderRepository.starFolder(folder.id, !folder.isStarred)
            refresh()
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            fileRepository.deleteFile(fileId)
            refresh()
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId)
            refresh()
        }
    }

    fun downloadFile(file: FileItem) {
        downloadRepository.enqueueDownload(file.id, file.name)
        _uiState.update { it.copy(successMessage = "Mengunduh ${file.name} di latar belakang...") }
    }

    // Multi-selection
    fun toggleFileSelection(fileId: String) {
        _uiState.update { state ->
            val set = state.selectedFileIds.toMutableSet()
            if (set.contains(fileId)) set.remove(fileId) else set.add(fileId)
            state.copy(selectedFileIds = set)
        }
    }

    fun toggleFolderSelection(folderId: String) {
        _uiState.update { state ->
            val set = state.selectedFolderIds.toMutableSet()
            if (set.contains(folderId)) set.remove(folderId) else set.add(folderId)
            state.copy(selectedFolderIds = set)
        }
    }

    fun selectAll() {
        val allFileIds = _uiState.value.files.map { it.id }.toSet()
        val allFolderIds = _uiState.value.folders.map { it.id }.toSet()
        _uiState.update { it.copy(selectedFileIds = allFileIds, selectedFolderIds = allFolderIds) }
    }

    fun isAllSelected(): Boolean {
        val state = _uiState.value
        val total = state.files.size + state.folders.size
        val selected = state.selectedFileIds.size + state.selectedFolderIds.size
        return total > 0 && selected == total
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFileIds = emptySet(), selectedFolderIds = emptySet()) }
    }

    fun deleteSelectedItems() {
        val files = _uiState.value.selectedFileIds.toList()
        val folders = _uiState.value.selectedFolderIds.toList()
        if (files.isEmpty() && folders.isEmpty()) return

        viewModelScope.launch {
            fileRepository.batchDelete(files, folders)
            clearSelection()
            refresh()
        }
    }
}
