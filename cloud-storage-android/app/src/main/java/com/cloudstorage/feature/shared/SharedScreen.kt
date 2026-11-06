package com.cloudstorage.feature.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.core.designsystem.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScreen(
    onNavigateToPreview: (String) -> Unit,
    viewModel: SharedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val currentFiles = if (uiState.selectedTab == 0) uiState.withMeFiles else uiState.byMeFiles
    val currentFolders = if (uiState.selectedTab == 0) uiState.withMeFolders else uiState.byMeFolders

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BackgroundDark)) {
                TopAppBar(
                    title = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Group, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Text("Shared", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
                )

                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = BackgroundDark,
                    contentColor = PrimaryBlue,
                    divider = { HorizontalDivider(color = BorderDark) }
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Text(
                                "Shared with me",
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (uiState.selectedTab == 0) PrimaryBlue else TextMutedDark
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Text(
                                "Shared by me",
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (uiState.selectedTab == 1) PrimaryBlue else TextMutedDark
                            )
                        }
                    )
                }
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentFiles.isEmpty() && currentFolders.isEmpty()) {
                EmptyStateView(
                    title = if (uiState.selectedTab == 0) "Belum ada item dibagikan" else "Anda belum membagikan item",
                    description = if (uiState.selectedTab == 0)
                        "File dan folder yang dibagikan orang lain kepada Anda akan muncul di sini."
                    else "Bagikan file atau folder dengan email kolaborator untuk bekerja bersama.",
                    icon = Icons.Outlined.Share
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentFolders.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text("Folders", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMutedDark)
                        }
                        items(currentFolders, key = { "shared_fol_${it.id}" }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = {}
                            )
                        }
                    }

                    if (currentFiles.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text("Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMutedDark, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(currentFiles, key = { "shared_file_${it.id}" }) { file ->
                            FileCard(
                                file = file,
                                onClick = { onNavigateToPreview(file.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

