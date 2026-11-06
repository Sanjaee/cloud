package com.cloudstorage.ios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cloudstorage.ios.model.CollaboratorDto
import com.cloudstorage.ios.model.UserSearchItemDto
import com.cloudstorage.ios.network.CloudApi
import com.cloudstorage.ios.platform.PlatformBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IosShareDialog(
    title: String,
    isBatch: Boolean,
    resourceType: String = "file",
    resourceId: String = "",
    batchFileIds: List<String> = emptyList(),
    batchFolderIds: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(0) }
    var recipientInput by remember { mutableStateOf("") }
    var optionalMessage by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("viewer") }
    var roleMenuExpanded by remember { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<List<UserSearchItemDto>>(emptyList()) }
    var collaborators by remember { mutableStateOf<List<CollaboratorDto>>(emptyList()) }
    var isLoadingCollaborators by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    var publicLinkRole by remember { mutableStateOf("viewer") }
    var publicLinkUrl by remember { mutableStateOf<String?>(null) }

    // Load collaborators for single item
    LaunchedEffect(resourceType, resourceId, isBatch) {
        if (!isBatch && resourceId.isNotBlank()) {
            isLoadingCollaborators = true
            collaborators = CloudApi.getCollaborators(resourceType, resourceId)
            isLoadingCollaborators = false
        }
    }

    // Debounced user search
    LaunchedEffect(recipientInput) {
        val q = recipientInput.trim()
        if (q.length < 2) {
            suggestions = emptyList()
        } else {
            delay(250)
            suggestions = CloudApi.searchUsers(q)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
            color = SurfaceDark
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(SurfaceElevatedDark)
                                .border(1.dp, BorderDark, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column {
                            Text(
                                text = title,
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Native Cloudflare R2 zero-copy sharing",
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderDark)

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { activeTab = 0 }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = if (activeTab == 0) TextPrimaryDark else TextMutedDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Collaborators",
                                color = if (activeTab == 0) TextPrimaryDark else TextMutedDark,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    if (!isBatch) {
                        Column(
                            modifier = Modifier
                                .clickable { activeTab = 1 }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) TextPrimaryDark else TextMutedDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Public Link",
                                    color = if (activeTab == 1) TextPrimaryDark else TextMutedDark,
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == 1) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderDark)

                if (activeTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = recipientInput,
                                onValueChange = { recipientInput = it },
                                placeholder = {
                                    Text("Email or username...", fontSize = 12.sp, color = TextMutedDark)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Box {
                                Surface(
                                    modifier = Modifier
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                                        .clickable { roleMenuExpanded = true },
                                    color = SurfaceElevatedDark
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = selectedRole.replaceFirstChar { it.uppercase() },
                                            color = TextPrimaryDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = roleMenuExpanded,
                                    onDismissRequest = { roleMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceElevatedDark)
                                ) {
                                    listOf("viewer", "commenter", "editor").forEach { role ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = role.replaceFirstChar { it.uppercase() },
                                                    color = TextPrimaryDark,
                                                    fontSize = 12.sp
                                                )
                                            },
                                            onClick = {
                                                selectedRole = role
                                                roleMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Autocomplete suggestions
                        if (suggestions.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevatedDark)
                                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            ) {
                                suggestions.take(4).forEach { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                recipientInput = user.email
                                                suggestions = emptyList()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = user.username,
                                                color = TextPrimaryDark,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = user.email,
                                                color = TextSecondaryDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.NorthWest,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = optionalMessage,
                            onValueChange = { optionalMessage = it },
                            placeholder = {
                                Text("Optional message...", fontSize = 12.sp, color = TextMutedDark)
                            },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    val res = if (isBatch) {
                                        CloudApi.batchShare(
                                            fileIds = batchFileIds,
                                            folderIds = batchFolderIds,
                                            recipient = recipientInput,
                                            permission = selectedRole,
                                            message = optionalMessage
                                        )
                                    } else {
                                        CloudApi.shareResource(
                                            resourceType = resourceType,
                                            resourceId = resourceId,
                                            recipient = recipientInput,
                                            permission = selectedRole,
                                            message = optionalMessage
                                        )
                                    }
                                    isSubmitting = false
                                    res.onSuccess {
                                        onMessage("Akses berhasil dibagikan ke $recipientInput")
                                        recipientInput = ""
                                        optionalMessage = ""
                                        if (!isBatch) {
                                            collaborators = CloudApi.getCollaborators(resourceType, resourceId)
                                        } else {
                                            onDismiss()
                                        }
                                    }.onFailure { err ->
                                        onMessage(err.message ?: "Gagal membagikan akses")
                                    }
                                }
                            },
                            enabled = recipientInput.isNotBlank() && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = TextPrimaryDark
                                )
                            } else {
                                Text("Share Access", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (!isBatch) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "COLLABORATORS",
                                    color = TextMutedDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceElevatedDark)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("You", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Owner", color = TextMutedDark, fontSize = 10.sp)
                                        }
                                        Text("Owner", color = TextSecondaryDark, fontSize = 10.sp)
                                    }
                                }

                                items(collaborators) { collab ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceElevatedDark)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = collab.username ?: collab.email ?: "User",
                                                color = TextPrimaryDark,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!collab.email.isNullOrBlank()) {
                                                Text(collab.email, color = TextMutedDark, fontSize = 10.sp)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = collab.permission.replaceFirstChar { it.uppercase() },
                                                color = TextSecondaryDark,
                                                fontSize = 10.sp
                                            )
                                            val permId = collab.permissionId ?: collab.id
                                            if (permId != null) {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            CloudApi.revokeCollaborator(permId)
                                                            collaborators = CloudApi.getCollaborators(resourceType, resourceId)
                                                        }
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Hapus",
                                                        tint = TextMutedDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Public Link Tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Buat tautan publik agar siapa pun yang memiliki tautan dapat mengakses item ini.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    CloudApi.createPublicLink(resourceType, resourceId, publicLinkRole)
                                        .onSuccess { url ->
                                            publicLinkUrl = url
                                            PlatformBridge.copyToClipboard(url)
                                            onMessage("Tautan publik disalin ke clipboard")
                                        }
                                        .onFailure { err ->
                                            onMessage(err.message ?: "Gagal membuat tautan")
                                        }
                                    isSubmitting = false
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Generate & Copy Public Link", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (!publicLinkUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevatedDark)
                                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = publicLinkUrl!!,
                                    color = PrimaryBlue,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        PlatformBridge.copyToClipboard(publicLinkUrl!!)
                                        onMessage("Tautan disalin!")
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
