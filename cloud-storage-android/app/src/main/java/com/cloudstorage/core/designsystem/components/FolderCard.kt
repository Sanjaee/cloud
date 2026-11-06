package com.cloudstorage.core.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.domain.model.FolderItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: FolderItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onStarClick: (Boolean) -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val folderColor = remember(folder.color) {
        if (!folder.color.isNullOrEmpty()) {
            try {
                Color(android.graphics.Color.parseColor(folder.color))
            } catch (e: Exception) {
                FolderDefaultColor
            }
        } else FolderDefaultColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else SurfaceDark)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else BorderDark,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = folderColor,
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = folder.name,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Folder options",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceElevatedDark)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (folder.isStarred) "Hapus dari Berbintang" else "Tambah ke Berbintang",
                                color = TextPrimaryDark,
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            showMenu = false
                            onStarClick(!folder.isStarred)
                        },
                        leadingIcon = {
                            Icon(
                                if (folder.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = if (folder.isStarred) AccentAmber else TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ubah Nama", color = TextPrimaryDark, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.DriveFileRenameOutline,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    HorizontalDivider(color = BorderDark)

                    DropdownMenuItem(
                        text = { Text("Hapus Folder", color = AccentRed, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
