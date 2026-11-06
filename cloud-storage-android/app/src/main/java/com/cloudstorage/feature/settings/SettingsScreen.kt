package com.cloudstorage.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                        fontSize = 18.sp,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimaryDark
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Success & Error Banners
            if (uiState.successMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                            Text(
                                text = uiState.successMessage ?: "",
                                color = AccentEmerald,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(20.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = AccentRed,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 1. User Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.2f))
                                    .border(1.5.dp, PrimaryBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = (uiState.user?.username?.firstOrNull() ?: uiState.user?.email?.firstOrNull() ?: 'U').uppercaseChar()
                                Text(
                                    text = initial.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.user?.username ?: "User",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = uiState.user?.email ?: "",
                                    fontSize = 13.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Storage Usage Bar
                        val quota = uiState.quota
                        val used = quota?.usedStorage ?: 0L
                        val total = quota?.totalQuota ?: (10L * 1024 * 1024 * 1024)
                        val percent = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Storage: ${formatBytes(used)} dari ${formatBytes(total)}",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                                Text(
                                    text = "${(percent * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                            }
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PrimaryBlue,
                                trackColor = SurfaceElevatedDark
                            )
                        }

                        // Logout Button
                        Button(
                            onClick = { showLogoutDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentRed.copy(alpha = 0.15f),
                                contentColor = AccentRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, AccentRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Keluar dari Akun (Logout)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Active Sessions Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Devices, null, tint = TextPrimaryDark, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Active Sessions (${uiState.sessions.size})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                Text(
                                    text = "Devices that currently have active session cookies or API tokens.",
                                    fontSize = 11.sp,
                                    color = TextMutedDark
                                )
                            }

                            if (uiState.sessions.size > 1) {
                                OutlinedButton(
                                    onClick = { showRevokeOthersDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sign Out Others", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Sessions list
                        uiState.sessions.forEach { session ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceElevatedDark, RoundedCornerShape(10.dp))
                                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = session.ipAddress ?: "Unknown IP",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextPrimaryDark
                                            )
                                            if (session.isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(AccentEmerald.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                        .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Current",
                                                        fontSize = 10.sp,
                                                        color = AccentEmerald,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = session.userAgent ?: "Android App",
                                            fontSize = 11.sp,
                                            color = TextMutedDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!session.isCurrent) {
                                        IconButton(
                                            onClick = { viewModel.revokeSession(session.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Revoke session",
                                                tint = TextMutedDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Change Password Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Change Password",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Text(
                            text = "Passwords are encrypted with state-of-the-art Argon2id hashing. Changing your password revokes other sessions.",
                            fontSize = 11.sp,
                            color = TextMutedDark,
                            lineHeight = 15.sp
                        )

                        // Current password
                        OutlinedTextField(
                            value = currPassword,
                            onValueChange = { currPassword = it },
                            label = { Text("Current Password", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevatedDark,
                                unfocusedContainerColor = SurfaceElevatedDark,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // New password
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password (8+ chars)", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevatedDark,
                                unfocusedContainerColor = SurfaceElevatedDark,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Confirm new password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceElevatedDark,
                                unfocusedContainerColor = SurfaceElevatedDark,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(10.dp),
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
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = BackgroundDark
                            ),
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(40.dp)
                        ) {
                            Text("Update Password", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 4. Two-Factor Authentication (TOTP) Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Lock, null, tint = TextPrimaryDark, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Two-Factor Authentication (TOTP)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (uiState.isMfaEnabled) AccentEmerald.copy(alpha = 0.2f) else BorderDark,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (uiState.isMfaEnabled) AccentEmerald.copy(alpha = 0.4f) else BorderDark,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (uiState.isMfaEnabled) "Enabled" else "Disabled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isMfaEnabled) AccentEmerald else TextSecondaryDark
                                )
                            }
                        }

                        Text(
                            text = "Require a 6-digit TOTP code from Google Authenticator, Authy, or 1Password when signing in.",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )

                        if (uiState.isMfaEnabled) {
                            // Card: Akun Anda Dilindungi 2FA
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentEmerald.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.dp, AccentEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Shield,
                                            null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Akun Anda Dilindungi 2FA",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = "Kode autentikasi dari ponsel Anda aktif untuk setiap sesi login.",
                                                fontSize = 11.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.openDisableMfaDialog() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Nonaktifkan 2FA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.openMfaSetupDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Aktifkan Two-Factor Authentication", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 5. Recent Security Activity Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Recent Security Activity",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Audit trail of logins, password resets, and security modifications.",
                                fontSize = 11.sp,
                                color = TextMutedDark
                            )
                        }

                        if (uiState.securityEvents.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat aktivitas keamanan.",
                                fontSize = 12.sp,
                                color = TextMutedDark,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            uiState.securityEvents.take(10).forEach { event ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatEventType(event.eventType),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = formatTimestamp(event.createdAt),
                                            fontSize = 11.sp,
                                            color = TextMutedDark
                                        )
                                    }
                                    if (!event.ipAddress.isNullOrBlank()) {
                                        Text(
                                            text = "IP: ${event.ipAddress}",
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 8.dp),
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
            title = { Text("Konfirmasi Logout", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ini?", color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Revoke Other Sessions Confirmation Dialog
    if (showRevokeOthersDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeOthersDialog = false },
            title = { Text("Sign Out Others", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = { Text("Cabut semua sesi lain? Anda akan tetap login di perangkat ini.", color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeOthersDialog = false
                        viewModel.revokeOtherSessions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Cabut Sesi Lain", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeOthersDialog = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Disable 2FA Dialog
    if (uiState.showMfaDisableDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDisableMfaDialog() },
            title = { Text("Nonaktifkan 2FA", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Masukkan 6-digit kode TOTP, Backup Code, atau Password akun Anda:", fontSize = 12.sp, color = TextSecondaryDark)
                    OutlinedTextField(
                        value = disableCodeInput,
                        onValueChange = { disableCodeInput = it },
                        label = { Text("Kode / Password") },
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
                    Text("Nonaktifkan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDisableMfaDialog() }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Setup 2FA Dialog
    if (uiState.showMfaSetupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeMfaSetupDialog() },
            title = { Text("Setup Two-Factor Authentication", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Masukkan Secret Key ini ke aplikasi Google Authenticator Anda:", fontSize = 12.sp, color = TextSecondaryDark)
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
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            letterSpacing = 1.sp
                        )
                    }

                    Text("2. Masukkan 6-digit kode konfirmasi dari Authenticator:", fontSize = 12.sp, color = TextSecondaryDark)
                    OutlinedTextField(
                        value = enableCodeInput,
                        onValueChange = { enableCodeInput = it },
                        label = { Text("6-digit TOTP") },
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
                    Text("Konfirmasi & Aktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeMfaSetupDialog() }) {
                    Text("Batal", color = TextSecondaryDark)
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
