package com.cloudstorage.feature.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.core.designsystem.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var itemToPermanentDelete by remember { mutableStateOf<Pair<String, String>?>(null) } // type, id

    val isSelecting = uiState.selectedFileIds.isNotEmpty() || uiState.selectedFolderIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text(
                            text = "${uiState.selectedFileIds.size + uiState.selectedFolderIds.size} dipilih",
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                    } else {
                        Text(
                            text = "Trash",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    if (isSelecting) {
                        IconButton(onClick = { viewModel.restoreSelected() }) {
                            Icon(Icons.Outlined.Restore, "Restore Selected", tint = AccentEmerald)
                        }
                        IconButton(onClick = { viewModel.deletePermanentlySelected() }) {
                            Icon(Icons.Outlined.DeleteForever, "Delete Permanently", tint = AccentRed)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancel", tint = TextSecondaryDark)
                        }
                    } else if (uiState.files.isNotEmpty() || uiState.folders.isNotEmpty()) {
                        Button(
                            onClick = { showEmptyTrashDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentRed.copy(alpha = 0.15f),
                                contentColor = AccentRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Empty Trash", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.files.isEmpty() && uiState.folders.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryBlue
                )
            } else if (uiState.files.isEmpty() && uiState.folders.isEmpty()) {
                EmptyStateView(
                    title = "Trash is empty",
                    description = "Deleted files and folders appear here until restored or permanently purged.",
                    icon = Icons.Default.DeleteOutline
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Folders in Trash
                    if (uiState.folders.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Deleted Folders",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMutedDark
                            )
                        }

                        items(uiState.folders, key = { "trash_folder_${it.id}" }) { folder ->
                            val isSelected = uiState.selectedFolderIds.contains(folder.id)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SurfaceElevatedDark else SurfaceDark)
                                    .border(1.dp, if (isSelected) PrimaryBlue else BorderDark, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSelecting) viewModel.toggleFolderSelection(folder.id)
                                    }
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, null, tint = FolderDefaultColor, modifier = Modifier.size(24.dp))
                                        Text(
                                            text = folder.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.restoreSingleItem("folder", folder.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.Restore, "Restore", tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { itemToPermanentDelete = Pair("folder", folder.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.DeleteForever, "Delete Permanently", tint = AccentRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Files in Trash
                    if (uiState.files.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Deleted Files",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMutedDark,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(uiState.files, key = { "trash_file_${it.id}" }) { file ->
                            val isSelected = uiState.selectedFileIds.contains(file.id)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SurfaceElevatedDark else SurfaceDark)
                                    .border(1.dp, if (isSelected) PrimaryBlue else BorderDark, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSelecting) viewModel.toggleFileSelection(file.id)
                                    }
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Thumbnail / icon
                                    FileCard(
                                        file = file,
                                        isSelected = isSelected,
                                        onClick = { viewModel.toggleFileSelection(file.id) }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.restoreSingleItem("file", file.id) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Outlined.Restore, null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", fontSize = 11.sp, color = AccentEmerald)
                                        }

                                        TextButton(
                                            onClick = { itemToPermanentDelete = Pair("file", file.id) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Outlined.DeleteForever, null, tint = AccentRed, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Purge", fontSize = 11.sp, color = AccentRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Empty Trash Dialog
    if (showEmptyTrashDialog) {
        ShadcnAlertDialog(
            title = "Empty Trash?",
            description = "All items in trash will be permanently purged from Cloudflare R2 storage. This action cannot be undone.",
            confirmText = "Empty All",
            variant = DialogVariant.DESTRUCTIVE,
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }

    // Single item permanent delete dialog
    itemToPermanentDelete?.let { (type, id) ->
        ShadcnAlertDialog(
            title = "Delete Permanently?",
            description = "This item will be deleted forever from cloud storage and your storage quota will be freed.",
            confirmText = "Delete Forever",
            variant = DialogVariant.DESTRUCTIVE,
            onConfirm = {
                viewModel.deleteSingleItemPermanently(type, id)
                itemToPermanentDelete = null
            },
            onDismiss = { itemToPermanentDelete = null }
        )
    }
}

