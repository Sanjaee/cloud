package com.cloudstorage.feature.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
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
import com.cloudstorage.core.designsystem.components.EmptyStateView
import com.cloudstorage.core.designsystem.components.FileCard
import com.cloudstorage.core.designsystem.components.FileSkeletonGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    onNavigateToPreview: (String) -> Unit,
    viewModel: RecentViewModel = hiltViewModel()
) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Text("Recent", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
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
            if (recentFiles.isEmpty() && isLoading) {
                FileSkeletonGrid()
            } else if (recentFiles.isEmpty()) {
                EmptyStateView(
                    title = "No recent files",
                    description = "Files you open, upload or modify will appear here for fast access.",
                    icon = Icons.Default.Schedule
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentFiles, key = { it.id }) { file ->
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

