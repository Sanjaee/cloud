package com.cloudstorage.feature.starred

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
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
fun StarredScreen(
    onNavigateToPreview: (String) -> Unit,
    viewModel: StarredViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Star, null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                        Text("Starred", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
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
            if (uiState.files.isEmpty() && uiState.folders.isEmpty()) {
                EmptyStateView(
                    title = "No starred items",
                    description = "Star files and folders to easily find them later here.",
                    icon = Icons.Default.StarOutline
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.folders.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text("Starred Folders", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMutedDark)
                        }
                        items(uiState.folders, key = { "star_fol_${it.id}" }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = {},
                                onStarClick = { viewModel.unstarFolder(folder) }
                            )
                        }
                    }

                    if (uiState.files.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text("Starred Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMutedDark, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(uiState.files, key = { "star_file_${it.id}" }) { file ->
                            FileCard(
                                file = file,
                                onClick = { onNavigateToPreview(file.id) },
                                onStarClick = { viewModel.unstarFile(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

