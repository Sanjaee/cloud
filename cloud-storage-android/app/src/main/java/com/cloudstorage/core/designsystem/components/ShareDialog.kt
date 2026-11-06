package com.cloudstorage.core.designsystem.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.data.remote.dto.UserSearchItemDto
import com.cloudstorage.domain.model.Collaborator

@Composable
fun ShareDialog(
    title: String,
    subtitle: String,
    isBatch: Boolean,
    collaborators: List<Collaborator>,
    searchResults: List<UserSearchItemDto>,
    publicLinkUrl: String,
    isSharing: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearchResults: () -> Unit,
    onShareSubmit: (recipient: String, permission: String, message: String?) -> Unit,
    onRevokeCollaborator: (userId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Collaborators, 1 = Public Link
    var recipientInput by remember { mutableStateOf("") }
    var selectedPermission by remember { mutableStateOf("viewer") }
    var permissionMenuExpanded by remember { mutableStateOf(false) }
    var optionalMessage by remember { mutableStateOf("") }
    var linkCopied by remember { mutableStateOf(false) }

    // Clear input when share succeeds on single item
    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank() && !isBatch) {
            recipientInput = ""
            optionalMessage = ""
            onClearSearchResults()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevatedDark)
                                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBatch) Icons.Outlined.Layers else Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = TextMutedDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMutedDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderDark)

                // Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Collaborators Tab
                    Column(
                        modifier = Modifier
                            .clickable { activeTab = 0 }
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color.White else TextMutedDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Collaborators",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = if (activeTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (activeTab == 0) Color.White else TextMutedDark
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(105.dp)
                                .height(2.dp)
                                .background(if (activeTab == 0) Color.White else Color.Transparent)
                        )
                    }

                    // Public Link Tab (only for single item)
                    if (!isBatch) {
                        Column(
                            modifier = Modifier
                                .clickable { activeTab = 1 }
                                .padding(top = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) Color.White else TextMutedDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Public Link",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = if (activeTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (activeTab == 1) Color.White else TextMutedDark
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(2.dp)
                                    .background(if (activeTab == 1) Color.White else Color.Transparent)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderDark)

                if (activeTab == 0) {
                    // Tab 1: Collaborators
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!errorMessage.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = errorMessage,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = AccentRed
                                )
                            }
                        }

                        if (!successMessage.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentEmerald.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Check, null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                Text(
                                    text = successMessage,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = AccentEmerald
                                )
                            }
                        }

                        // Email/username input + Permission selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = recipientInput,
                                onValueChange = {
                                    recipientInput = it
                                    onSearchQueryChange(it)
                                },
                                placeholder = {
                                    Text(
                                        "Email or username...",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = TextMutedDark
                                    )
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = TextPrimaryDark
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF18181B),
                                    unfocusedContainerColor = Color(0xFF18181B),
                                    focusedBorderColor = Color(0xFF3F3F46),
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Role dropdown button
                            Box {
                                Row(
                                    modifier = Modifier
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF18181B))
                                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                                        .clickable { permissionMenuExpanded = true }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selectedPermission.replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimaryDark
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select role",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = permissionMenuExpanded,
                                    onDismissRequest = { permissionMenuExpanded = false },
                                    modifier = Modifier
                                        .background(SurfaceElevatedDark)
                                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                                ) {
                                    listOf("viewer" to "Viewer", "commenter" to "Commenter", "editor" to "Editor").forEach { (value, label) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    color = if (selectedPermission == value) PrimaryBlue else TextPrimaryDark,
                                                    fontWeight = if (selectedPermission == value) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedPermission = value
                                                permissionMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Autocomplete suggestions list
                        if (searchResults.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF18181B))
                                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            ) {
                                searchResults.take(4).forEachIndexed { idx, user ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                recipientInput = user.email
                                                onClearSearchResults()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = user.username,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = user.email,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            color = TextMutedDark
                                        )
                                    }
                                    if (idx < searchResults.take(4).lastIndex) {
                                        HorizontalDivider(color = BorderDark.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }

                        // Optional message input
                        OutlinedTextField(
                            value = optionalMessage,
                            onValueChange = { optionalMessage = it },
                            placeholder = {
                                Text(
                                    "Optional message...",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = TextMutedDark
                                )
                            },
                            maxLines = 2,
                            textStyle = TextStyle(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = TextPrimaryDark
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF18181B),
                                unfocusedContainerColor = Color(0xFF18181B),
                                focusedBorderColor = Color(0xFF3F3F46),
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                        )

                        // Share Access Button (White button with dark text, matching web UI)
                        Button(
                            onClick = {
                                if (recipientInput.isNotBlank()) {
                                    onShareSubmit(
                                        recipientInput.trim(),
                                        selectedPermission,
                                        optionalMessage.trim().takeIf { it.isNotEmpty() }
                                    )
                                }
                            },
                            enabled = !isSharing && recipientInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF09090B),
                                disabledContainerColor = Color(0xFF3F3F46),
                                disabledContentColor = Color(0xFFA1A1AA)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            if (isSharing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF09090B)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sharing...",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Share Access",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Collaborators list (for single item)
                        if (!isBatch) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "COLLABORATORS",
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMutedDark,
                                    letterSpacing = 0.6.sp
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 170.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Owner row
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF18181B), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "You",
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = "Owner",
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                color = TextMutedDark
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF27272A), RoundedCornerShape(5.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Owner",
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }
                                }

                                // Collaborators
                                items(collaborators, key = { it.id }) { collab ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF18181B), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = collab.username ?: "User",
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = collab.email ?: "",
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                color = TextMutedDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF27272A), RoundedCornerShape(5.dp))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = collab.permission.replaceFirstChar { it.uppercase() },
                                                    fontSize = 10.sp,
                                                    lineHeight = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimaryDark
                                                )
                                            }

                                            IconButton(
                                                onClick = { onRevokeCollaborator(collab.userId) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Revoke access",
                                                    tint = TextMutedDark,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tab 2: Public Share Link
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Anyone with this link can view and download this item directly from Cloudflare R2 storage.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = TextSecondaryDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF18181B))
                                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = publicLinkUrl.ifBlank { "Generating link..." },
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (publicLinkUrl.isNotBlank()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Share Link", publicLinkUrl))
                                        linkCopied = true
                                    }
                                },
                                enabled = publicLinkUrl.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF09090B)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (linkCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (linkCopied) "Copied" else "Copy",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

