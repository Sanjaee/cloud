package com.cloudstorage.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRevokeOthersDialog by remember { mutableStateOf(false) }

    // Change Password inputs
    var currPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // 2FA Dialog inputs
    var disableCodeInput by remember { mutableStateOf("") }
    var enableCodeInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Security",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            // Success & Error Banners
            if (uiState.successMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentEmerald.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                        Text(
                            text = uiState.successMessage ?: "",
                            color = AccentEmerald,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = AccentRed,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, null, tint = AccentRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // 1. User Profile Card (Compact)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.2f))
                                        .border(1.dp, PrimaryBlue.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = (uiState.user?.username?.firstOrNull()
                                        ?: uiState.user?.email?.firstOrNull()
                                        ?: 'U').uppercaseChar()
                                    Text(
                                        text = initial.toString(),
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.user?.username ?: "User",
                                        fontSize = 14.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = uiState.user?.email ?: "",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = TextSecondaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Compact Logout Button right in Profile Header
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AccentRed.copy(alpha = 0.12f),
                                    contentColor = AccentRed
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Logout",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Compact Storage Usage Bar
                        val quota = uiState.quota
                        val used = quota?.usedStorage ?: 0L
                        val total = quota?.totalQuota ?: (10L * 1024 * 1024 * 1024)
                        val percent = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Storage: ${formatBytes(used)} / ${formatBytes(total)}",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = TextMutedDark
                                )
                                Text(
                                    text = "${(percent * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                            }
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PrimaryBlue,
                                trackColor = SurfaceElevatedDark
                            )
                        }
                    }
                }
            }

            // 2. Active Sessions Section (Compact)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Smartphone, null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Active Sessions (${uiState.sessions.size})",
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (uiState.sessions.size > 1) {
                                OutlinedButton(
                                    onClick = { showRevokeOthersDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AccentRed.copy(alpha = 0.08f),
                                        contentColor = AccentRed
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sign Out Others", fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Text(
                            text = "Devices that currently have active session cookies or API tokens.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = TextMutedDark
                        )

                        // Sessions list (Compact rows)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            uiState.sessions.forEach { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF16161A), RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderDark.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceElevatedDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = session.ipAddress ?: "Unknown IP",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                color = TextPrimaryDark,
                                                maxLines = 1
                                            )
                                            if (session.isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(AccentEmerald.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
                                                        .border(1.dp, AccentEmerald.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "Current",
                                                        fontSize = 9.sp,
                                                        lineHeight = 11.sp,
                                                        color = AccentEmerald,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = session.userAgent ?: "ZacodeStorage-Android/1.0.0",
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            color = TextMutedDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!session.isCurrent) {
                                        IconButton(
                                            onClick = { viewModel.revokeSession(session.id) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Revoke session",
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
            }

            // 3. Two-Factor Authentication (TOTP) Section (Compact)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Lock, null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Two-Factor Authentication (TOTP)",
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (uiState.isMfaEnabled) AccentEmerald.copy(alpha = 0.16f) else SurfaceElevatedDark,
                                        RoundedCornerShape(5.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (uiState.isMfaEnabled) AccentEmerald.copy(alpha = 0.35f) else BorderDark,
                                        RoundedCornerShape(5.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (uiState.isMfaEnabled) "Enabled" else "Disabled",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isMfaEnabled) AccentEmerald else TextSecondaryDark
                                )
                            }
                        }

                        Text(
                            text = "Require a 6-digit TOTP code from Google Authenticator, Authy, or 1Password when signing in.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = TextMutedDark
                        )

                        if (uiState.isMfaEnabled) {
                            // Compact Protected Status Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentEmerald.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .border(1.dp, AccentEmerald.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AccentEmerald.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Shield,
                                            null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Akun Anda Dilindungi 2FA",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Kode TOTP ponsel aktif untuk setiap sesi login.",
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            color = TextSecondaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.openDisableMfaDialog() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AccentRed.copy(alpha = 0.08f),
                                        contentColor = AccentRed
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Nonaktifkan 2FA",
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.openMfaSetupDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF09090B)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Security, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Enable Two-Factor Authentication",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Change Password Section (Compact)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.VpnKey, null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Change Password",
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Text(
                            text = "Passwords are encrypted with Argon2id hashing. Changing your password revokes other sessions.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = TextMutedDark
                        )

                        OutlinedTextField(
                            value = currPassword,
                            onValueChange = { currPassword = it },
                            placeholder = { Text("Current Password", fontSize = 12.sp, lineHeight = 15.sp, color = TextMutedDark) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, color = TextPrimaryDark),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF16161A),
                                unfocusedContainerColor = Color(0xFF16161A),
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = { Text("New Password (8+ chars)", fontSize = 12.sp, lineHeight = 15.sp, color = TextMutedDark) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, color = TextPrimaryDark),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF16161A),
                                unfocusedContainerColor = Color(0xFF16161A),
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = { Text("Confirm New Password", fontSize = 12.sp, lineHeight = 15.sp, color = TextMutedDark) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, color = TextPrimaryDark),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF16161A),
                                unfocusedContainerColor = Color(0xFF16161A),
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.changePassword(currPassword, newPassword, confirmPassword)
                                currPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            },
                            enabled = !uiState.isActionLoading && currPassword.isNotBlank() && newPassword.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF09090B),
                                disabledContainerColor = Color(0xFF27272A),
                                disabledContentColor = Color(0xFF71717A)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(34.dp)
                        ) {
                            Text("Update Password", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }

            // 5. Recent Security Activity Section (Compact List)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Recent Security Activity",
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Audit trail of logins, password resets, and security modifications.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = TextMutedDark
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        if (uiState.securityEvents.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat aktivitas keamanan.",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = TextMutedDark,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            val eventsList = uiState.securityEvents.take(10)
                            eventsList.forEachIndexed { index, event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formatEventType(event.eventType),
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!event.ipAddress.isNullOrBlank()) {
                                            Text(
                                                text = "IP: ${event.ipAddress}",
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                color = TextMutedDark,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = formatTimestamp(event.createdAt),
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        color = TextMutedDark,
                                        maxLines = 1
                                    )
                                }

                                if (index < eventsList.lastIndex) {
                                    HorizontalDivider(
                                        color = BorderDark.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ini?", fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Keluar", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Revoke Other Sessions Confirmation Dialog
    if (showRevokeOthersDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeOthersDialog = false },
            title = { Text("Sign Out Others", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = { Text("Cabut semua sesi lain? Anda akan tetap login di perangkat ini.", fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeOthersDialog = false
                        viewModel.revokeOtherSessions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Cabut Sesi Lain", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeOthersDialog = false }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Disable 2FA Dialog
    if (uiState.showMfaDisableDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDisableMfaDialog() },
            title = { Text("Nonaktifkan 2FA", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Masukkan 6-digit kode TOTP, Backup Code, atau Password akun Anda:",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondaryDark
                    )
                    OutlinedTextField(
                        value = disableCodeInput,
                        onValueChange = { disableCodeInput = it },
                        placeholder = { Text("Kode TOTP / Backup Code / Password", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disableMfa(disableCodeInput)
                        disableCodeInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Nonaktifkan", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDisableMfaDialog() }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Setup 2FA Dialog
    if (uiState.showMfaSetupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeMfaSetupDialog() },
            title = { Text("Setup Two-Factor Authentication", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "1. Masukkan Secret Key ini ke aplikasi Google Authenticator Anda:",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondaryDark
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevatedDark, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = uiState.mfaSecret ?: "",
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        "2. Masukkan 6-digit kode konfirmasi dari Authenticator:",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextSecondaryDark
                    )
                    OutlinedTextField(
                        value = enableCodeInput,
                        onValueChange = { enableCodeInput = it },
                        placeholder = { Text("6-digit TOTP", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmEnableMfa(enableCodeInput)
                        enableCodeInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Konfirmasi & Aktifkan", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeMfaSetupDialog() }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

private fun formatEventType(type: String): String {
    return when (type) {
        "google_oauth_login_success" -> "Google OAuth Login Success"
        "login_success" -> "Login Success"
        "mfa_enabled" -> "Mfa Enabled"
        "mfa_disabled" -> "Mfa Disabled"
        "password_changed" -> "Password Changed"
        "password_reset_completed" -> "Password Reset Completed"
        else -> type.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

private fun formatTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = parser.parse(iso)
        val formatter = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US)
        if (date != null) formatter.format(date) else iso
    } catch (_: Exception) {
        iso
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}
