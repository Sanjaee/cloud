package com.cloudstorage.feature.drive

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.core.designsystem.components.*
import com.cloudstorage.domain.model.FileItem
import com.cloudstorage.domain.model.FolderItem
import com.cloudstorage.navigation.NavigationTransitions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateToShare: (String, String) -> Unit, // type, id
    onNavigateToSettings: () -> Unit = {},
    viewModel: DriveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameToCreate by remember { mutableStateOf("") }

    var itemToDelete by remember { mutableStateOf<Pair<String, String>?>(null) } // type, id
    var itemToRename by remember { mutableStateOf<Triple<String, String, String>?>(null) } // type, id, currentName
    var renameInput by remember { mutableStateOf("") }

    // Multi-select file picker supporting single or multiple files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadFiles(uris, context)
        }
    }

    // Handle Android system back button when inside a subfolder
    BackHandler(enabled = uiState.currentFolderId != null) {
        viewModel.navigateUp()
    }

    // Show Snackbars for error or success notifications
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val isSelecting = uiState.selectedFileIds.isNotEmpty() || uiState.selectedFolderIds.isNotEmpty()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (uiState.currentFolderId != null) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke folder sebelumnya",
                                tint = TextPrimaryDark
                            )
                        }
                    }
                },
                title = {
                    if (isSelecting) {
                        Text(
                            text = "${uiState.selectedFileIds.size + uiState.selectedFolderIds.size} dipilih",
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                    } else {
                        val headerTitle = if (uiState.currentFolderId != null) {
                            uiState.breadcrumbs.lastOrNull()?.name ?: "Folder"
                        } else {
                            "My Drive"
                        }
                        Text(
                            text = headerTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    if (isSelecting) {
                        val isAllSelected = viewModel.isAllSelected()
                        IconButton(onClick = {
                            if (isAllSelected) viewModel.clearSelection()
                            else viewModel.selectAll()
                        }) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua",
                                tint = TextPrimaryDark
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(Icons.Default.Delete, "Delete selected", tint = AccentRed)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancel selection", tint = TextSecondaryDark)
                        }
                    } else {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, "Search", tint = TextPrimaryDark)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.2f))
                                    .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile & Security",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf(false) }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fabExpanded) {
                    SmallFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            showCreateFolderDialog = true
                        },
                        containerColor = SurfaceElevatedDark,
                        contentColor = TextPrimaryDark,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, "New Folder")
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            filePickerLauncher.launch("*/*")
                        },
                        containerColor = SurfaceElevatedDark,
                        contentColor = TextPrimaryDark,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Outlined.UploadFile, "Upload File")
                    }
                }

                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.Add, "Actions")
                }
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Breadcrumbs Bar
            BreadcrumbBar(
                breadcrumbs = uiState.breadcrumbs,
                onBreadcrumbClick = { folderId -> viewModel.openFolder(folderId) }
            )

            // Live Upload Progress Indicator Banner
            if (uiState.isUploading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mengunggah \"${uiState.uploadingFileName ?: "file"}\"...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { uiState.uploadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = PrimaryBlue,
                                trackColor = BorderDark
                            )
                        }
                    }
                }
            }

            // Pull-To-Refresh Container wrapping the Content Grid
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.pullRefresh() },
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = uiState.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = SurfaceElevatedDark,
                        color = PrimaryBlue
                    )
                }
            ) {
                // Content Grid with smooth Gojek-style slide transition between folders
                AnimatedContent(
                    targetState = uiState.currentFolderId,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(
                                durationMillis = NavigationTransitions.DURATION_SLIDE,
                                easing = NavigationTransitions.StandardEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(NavigationTransitions.DURATION_FADE)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(
                                durationMillis = NavigationTransitions.DURATION_SLIDE,
                                easing = NavigationTransitions.StandardEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(NavigationTransitions.DURATION_FADE)
                        )
                    },
                    label = "drive_folder_transition",
                    modifier = Modifier.fillMaxSize()
                ) { _ ->
                    if (uiState.isLoading && uiState.folders.isEmpty() && uiState.files.isEmpty()) {
                        DriveSkeletonGrid()
                    } else if (uiState.folders.isEmpty() && uiState.files.isEmpty()) {
                        EmptyStateView(
                            title = "Folder ini kosong",
                            description = "Gunakan tombol + untuk mengunggah file baru atau membuat folder."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Folders Section
                            if (uiState.folders.isNotEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    Text(
                                        text = "Folders",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMutedDark,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                items(uiState.folders, key = { "folder_${it.id}" }) { folder ->
                                    val isSelected = uiState.selectedFolderIds.contains(folder.id)
                                    FolderCard(
                                        folder = folder,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelecting) {
                                                viewModel.toggleFolderSelection(folder.id)
                                            } else {
                                                viewModel.openFolder(folder.id)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleFolderSelection(folder.id) },
                                        onStarClick = { viewModel.toggleStarFolder(folder) },
                                        onRenameClick = {
                                            itemToRename = Triple("folder", folder.id, folder.name)
                                            renameInput = folder.name
                                        },
                                        onDeleteClick = {
                                            itemToDelete = Pair("folder", folder.id)
                                        }
                                    )
                                }
                            }

                            // Files Section
                            if (uiState.files.isNotEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    Text(
                                        text = "Files",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMutedDark,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                }

                                items(uiState.files, key = { "file_${it.id}" }) { file ->
                                    val isSelected = uiState.selectedFileIds.contains(file.id)
                                    FileCard(
                                        file = file,
                                        isSelected = isSelected,
                                        accessToken = viewModel.accessToken,
                                        onClick = {
                                            if (isSelecting) {
                                                viewModel.toggleFileSelection(file.id)
                                            } else {
                                                onNavigateToPreview(file.id)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleFileSelection(file.id) },
                                        onStarClick = { viewModel.toggleStarFile(file) },
                                        onDownloadClick = { viewModel.downloadFile(file) },
                                        onShareClick = { onNavigateToShare("file", file.id) },
                                        onRenameClick = {
                                            itemToRename = Triple("file", file.id, file.name)
                                            renameInput = file.name
                                        },
                                        onDeleteClick = {
                                            itemToDelete = Pair("file", file.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Buat Folder Baru", color = TextPrimaryDark, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = folderNameToCreate,
                    onValueChange = { folderNameToCreate = it },
                    placeholder = { Text("Nama Folder", color = TextMutedDark) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevatedDark,
                        unfocusedContainerColor = SurfaceElevatedDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameToCreate.isNotBlank()) {
                            viewModel.createFolder(folderNameToCreate.trim())
                            folderNameToCreate = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Rename Dialog
    itemToRename?.let { (type, id, _) ->
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Ubah Nama", color = TextPrimaryDark, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevatedDark,
                        unfocusedContainerColor = SurfaceElevatedDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            if (type == "file") viewModel.renameFile(id, renameInput.trim())
                            else viewModel.renameFolder(id, renameInput.trim())
                            itemToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { (type, id) ->
        ShadcnAlertDialog(
            title = "Pindahkan ke Sampah?",
            description = "Item ini akan dipindahkan ke tempat sampah dan dapat dipulihkan kapan saja sebelum dihapus permanen.",
            confirmText = "Pindahkan ke Sampah",
            variant = DialogVariant.DESTRUCTIVE,
            onConfirm = {
                if (type == "file") viewModel.deleteFile(id)
                else viewModel.deleteFolder(id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }
}
