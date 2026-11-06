package com.cloudstorage.feature.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: StorageViewModel = hiltViewModel()
) {
    val quota by viewModel.quota.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudQueue, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Text("Storage Quota", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Person, "Profile & Security", tint = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (isLoading && quota == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                val q = quota
                val used = q?.formattedUsed ?: "0 B"
                val total = q?.formattedTotal ?: "10 GB"
                val percent = q?.percentageUsed?.toFloat() ?: 0f

                // Main Quota Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$used used of $total",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${String.format("%.1f", percent)}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (percent > 85f) AccentRed else PrimaryBlue
                            )
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (percent > 85f) AccentRed else PrimaryBlue,
                            trackColor = SurfaceElevatedDark
                        )

                        Text(
                            text = "${q?.fileCount ?: 0} file tersimpan di Cloudflare R2 storage",
                            fontSize = 12.sp,
                            color = TextMutedDark
                        )
                    }
                }

                // Breakdown Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Rincian Penyimpanan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )

                        HorizontalDivider(color = BorderDark)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kapasitas Gratis", fontSize = 13.sp, color = TextSecondaryDark)
                            Text("10 GB", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimaryDark)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tersedia", fontSize = 13.sp, color = TextSecondaryDark)
                            Text(q?.formattedAvailable ?: "10 GB", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentEmerald)
                        }
                    }
                }
            }
        }
    }
}

