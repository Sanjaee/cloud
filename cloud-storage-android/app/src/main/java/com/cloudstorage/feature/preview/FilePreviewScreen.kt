package com.cloudstorage.feature.preview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cloudstorage.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilePreviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val file = uiState.file

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = file?.name ?: "File Preview",
                        maxLines = 1,
                        fontSize = 16.sp,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    file?.let {
                        IconButton(onClick = { viewModel.toggleStar() }) {
                            Icon(
                                if (it.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                "Star",
                                tint = if (it.isStarred) AccentAmber else TextSecondaryDark
                            )
                        }
                        IconButton(onClick = { viewModel.downloadFile() }) {
                            Icon(Icons.Outlined.Download, "Download", tint = TextPrimaryDark)
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
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryBlue
                )
            } else if (file != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Preview Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        if (file.isImage && !uiState.previewUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uiState.previewUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = file.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = file.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${file.mimeType} • ${file.formattedSize}",
                                    fontSize = 13.sp,
                                    color = TextSecondaryDark
                                )

                                if (!uiState.previewUrl.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.previewUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Buka File")
                                    }
                                }
                            }
                        }
                    }

                    // Metadata Bottom Sheet Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevatedDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Informasi File",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tipe", color = TextSecondaryDark, fontSize = 13.sp)
                                Text(file.mimeType, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ukuran", color = TextSecondaryDark, fontSize = 13.sp)
                                Text(file.formattedSize, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            file.createdAt?.let {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dibuat", color = TextSecondaryDark, fontSize = 13.sp)
                                    Text(it, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Gagal memuat detail file",
                        color = TextSecondaryDark,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Kembali")
                    }
                }
            }
        }
    }
}
