package com.cloudstorage.ios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudstorage.ios.model.*
import com.cloudstorage.ios.network.CloudApi
import com.cloudstorage.ios.platform.PlatformBridge
import kotlinx.coroutines.launch

@Composable
fun IosSettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<UserDto?>(null) }
    var quota by remember { mutableStateOf(QuotaResponseDto()) }
    var isMfaEnabled by remember { mutableStateOf(false) }
    var mfaSetupData by remember { mutableStateOf<MfaSetupResponseDto?>(null) }
    var backupCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var totpInput by remember { mutableStateOf("") }
    var showDisableMfaInput by remember { mutableStateOf(false) }
    var disableCodeInput by remember { mutableStateOf("") }
    var sessions by remember { mutableStateOf<List<SessionItemDto>>(emptyList()) }
    var events by remember { mutableStateOf<List<SecurityEventDto>>(emptyList()) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    fun refreshProfile() {
        scope.launch {
            user = CloudApi.getCurrentUser()
            quota = CloudApi.getQuota()
            isMfaEnabled = CloudApi.getMfaStatus()
            sessions = CloudApi.getSessions()
            events = CloudApi.getSecurityEvents()
        }
    }

    LaunchedEffect(Unit) {
        refreshProfile()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Profile & Security", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { refreshProfile() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondaryDark)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // 1. Account Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val email = user?.email ?: CloudApi.getStoredEmail() ?: "User"
                        val username = user?.username ?: CloudApi.getStoredUsername() ?: email.substringBefore("@")
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.2f))
                                .border(1.dp, PrimaryBlue.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username.take(1).uppercase(),
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = username,
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = email,
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                        ) {
                            Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Keluar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Quota bar
                    val used = quota.usedStorage
                    val total = quota.totalQuota.coerceAtLeast(1L)
                    val progress = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Storage Used", color = TextSecondaryDark, fontSize = 11.sp)
                            Text("${formatBytes(used)} / ${formatBytes(total)}", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = PrimaryBlue,
                            trackColor = SurfaceElevatedDark
                        )
                    }
                }
            }

            // 2. Two-Factor Authentication (TOTP) Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Text("Two-Factor Authentication (TOTP)", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isMfaEnabled) AccentEmerald.copy(alpha = 0.15f) else SurfaceElevatedDark)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isMfaEnabled) "Enabled" else "Disabled",
                                color = if (isMfaEnabled) AccentEmerald else TextMutedDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Require a 6-digit TOTP code from Google Authenticator, Authy, or 1Password when signing in.",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    if (isMfaEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentEmerald.copy(alpha = 0.08f))
                                .border(1.dp, AccentEmerald.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                                .padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Akun Anda Dilindungi 2FA", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Kode autentikasi aktif untuk setiap login.", color = TextSecondaryDark, fontSize = 10.sp)
                                }
                            }
                            if (!showDisableMfaInput) {
                                OutlinedButton(
                                    onClick = { showDisableMfaInput = true },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(7.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                                ) {
                                    Text("Nonaktifkan", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (showDisableMfaInput) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = disableCodeInput,
                                    onValueChange = { disableCodeInput = it },
                                    placeholder = { Text("Kode 6-digit TOTP", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            CloudApi.disableMfa(disableCodeInput)
                                                .onSuccess {
                                                    isMfaEnabled = false
                                                    showDisableMfaInput = false
                                                    disableCodeInput = ""
                                                    onMessage("2FA berhasil dinonaktifkan")
                                                }
                                                .onFailure { onMessage(it.message ?: "Gagal menonaktifkan 2FA") }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                                ) {
                                    Text("Konfirmasi", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        if (mfaSetupData == null) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        CloudApi.setupMfa()
                                            .onSuccess { mfaSetupData = it }
                                            .onFailure { onMessage(it.message ?: "Gagal menyiapkan 2FA") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Aktifkan 2FA (TOTP)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceElevatedDark)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Salin Secret Key ini ke Google Authenticator:", color = TextSecondaryDark, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mfaSetupData!!.secret,
                                        color = AccentEmerald,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            PlatformBridge.copyToClipboard(mfaSetupData!!.secret)
                                            onMessage("Secret key disalin")
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextPrimaryDark, modifier = Modifier.size(14.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = totpInput,
                                    onValueChange = { totpInput = it },
                                    placeholder = { Text("Masukkan 6-digit kode TOTP", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            CloudApi.enableMfa(totpInput)
                                                .onSuccess { codes ->
                                                    backupCodes = codes
                                                    isMfaEnabled = true
                                                    mfaSetupData = null
                                                    totpInput = ""
                                                    onMessage("2FA berhasil diaktifkan!")
                                                }
                                                .onFailure { onMessage(it.message ?: "Kode salah") }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                                ) {
                                    Text("Verifikasi & Aktifkan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    if (backupCodes.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevatedDark)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Simpan Backup Codes Anda:", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(backupCodes.joinToString("   "), color = TextPrimaryDark, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // 3. Active Sessions (Compact)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Active Sessions (${sessions.size})", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    sessions.take(5).forEach { sess ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevatedDark)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sess.ipAddress ?: "127.0.0.1", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(sess.userAgent ?: "ZacodeCloud-iOS", color = TextMutedDark, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (sess.isCurrent) {
                                Text("Current", color = AccentEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Recent Security Activity (Compact single-line rows)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Recent Security Activity", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    events.take(6).forEach { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.eventType.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("IP: ${ev.ipAddress ?: "-"}", color = TextMutedDark, fontSize = 10.sp)
                            }
                            Text(
                                text = (ev.createdAt ?: "").take(10),
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        }
                        HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = SurfaceDark,
            title = { Text("Keluar dari Akun?", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Sesi Anda pada perangkat iOS ini akan diakhiri.", color = TextSecondaryDark, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        scope.launch {
                            CloudApi.logout()
                            onLoggedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Keluar", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        )
    }
}
