package com.cloudstorage.core.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.domain.model.FileItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(
    file: FileItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onStarClick: (Boolean) -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    previewBaseUrl: String = "https://cloud-storage-api-tau.vercel.app/api/v1/downloads",
    accessToken: String? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SurfaceElevatedDark else SurfaceDark)
            .border(
                1.dp,
                if (isSelected) PrimaryBlue else BorderDark,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Preview / Icon Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                if (file.isImage) {
                    val previewUrl = "$previewBaseUrl/${file.id}/view"
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(previewUrl)
                            .apply {
                                if (!accessToken.isNullOrEmpty()) {
                                    addHeader("Authorization", "Bearer $accessToken")
                                }
                            }
                            .crossfade(true)
                            .build(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val icon = when {
                        file.isVideo -> Icons.Default.Videocam
                        file.isAudio -> Icons.Default.Audiotrack
                        file.isPdf -> Icons.Default.PictureAsPdf
                        file.mimeType.contains("zip") || file.mimeType.contains("compressed") -> Icons.Default.FolderZip
                        file.mimeType.contains("text") || file.mimeType.contains("json") -> Icons.Default.Description
                        else -> Icons.Default.InsertDriveFile
                    }
                    val iconColor = when {
                        file.isPdf -> AccentRed
                        file.isVideo -> PrimaryBlue
                        file.isAudio -> AccentAmber
                        else -> TextSecondaryDark
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Star indicator
                if (file.isStarred) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Starred",
                        tint = AccentAmber,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            // File Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(SurfaceElevatedDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download", color = TextPrimaryDark, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Download, null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp)) },
                            onClick = { showMenu = false; onDownloadClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", color = TextPrimaryDark, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Share, null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp)) },
                            onClick = { showMenu = false; onShareClick() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (file.isStarred) "Remove Star" else "Star", color = TextPrimaryDark, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    if (file.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = { showMenu = false; onStarClick(!file.isStarred) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", color = TextPrimaryDark, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp)) },
                            onClick = { showMenu = false; onRenameClick() }
                        )
                        HorizontalDivider(color = BorderDark)
                        DropdownMenuItem(
                            text = { Text("Delete", color = AccentRed, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = AccentRed, modifier = Modifier.size(18.dp)) },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }
                }
            }
        }
    }
}

