package com.cloudstorage.ios.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cloudstorage.ios.model.*
import com.cloudstorage.ios.network.CloudApi
import com.cloudstorage.ios.platform.PlatformBridge
import com.cloudstorage.ios.platform.PlatformOAuthWebView
import kotlinx.coroutines.launch

private enum class AppScreen {
    LOGIN, MFA_VERIFY, MAIN, SETTINGS
}

private enum class BottomTab {
    DRIVE, SHARED, TRASH
}

private data class ShareDialogTarget(
    val title: String,
    val isBatch: Boolean,
    val resourceType: String = "file",
    val resourceId: String = "",
    val batchFileIds: List<String> = emptyList(),
    val batchFolderIds: List<String> = emptyList()
)

@Composable
fun ZacodeCloudIosApp() {
    ZacodeIosTheme {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var currentScreen by remember {
            mutableStateOf(if (CloudApi.isLoggedIn()) AppScreen.MAIN else AppScreen.LOGIN)
        }
        var pendingMfaToken by remember { mutableStateOf("") }
        var pendingMfaEmail by remember { mutableStateOf("") }
        var pendingLoginPassword by remember { mutableStateOf("") }
        var isGoogleMfa by remember { mutableStateOf(true) }

        fun showToast(msg: String) {
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }

        Scaffold(
            containerColor = BackgroundDark,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.LOGIN -> IosLoginScreen(
                        onSuccess = { currentScreen = AppScreen.MAIN },
                        onMfaRequired = { token, email, password, fromGoogle ->
                            pendingMfaToken = token
                            pendingMfaEmail = email
                            pendingLoginPassword = password
                            isGoogleMfa = fromGoogle
                            currentScreen = AppScreen.MFA_VERIFY
                        },
                        onMessage = ::showToast
                    )

                    AppScreen.MFA_VERIFY -> IosTotpVerificationScreen(
                        email = pendingMfaEmail,
                        mfaToken = pendingMfaToken,
                        password = pendingLoginPassword,
                        isGoogleMfa = isGoogleMfa,
                        onSuccess = { currentScreen = AppScreen.MAIN },
                        onCancel = { currentScreen = AppScreen.LOGIN },
                        onMessage = ::showToast
                    )

                    AppScreen.MAIN -> IosMainWorkspaceScreen(
                        onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                        onMessage = ::showToast
                    )

                    AppScreen.SETTINGS -> IosSettingsScreen(
                        onBack = { currentScreen = AppScreen.MAIN },
                        onLoggedOut = { currentScreen = AppScreen.LOGIN },
                        onMessage = ::showToast
                    )
                }
            }
        }
    }
}

@Composable
private fun IosLoginScreen(
    onSuccess: () -> Unit,
    onMfaRequired: (mfaToken: String, email: String, password: String, fromGoogle: Boolean) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showGoogleOAuthSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevatedDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(26.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Welcome to Zacode Cloud", color = TextPrimaryDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Kotlin Multiplatform iOS Edition",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentRed.copy(alpha = 0.12f))
                        .border(1.dp, AccentRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    Text(errorMessage!!, color = AccentRed, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }
            }

            // Google OAuth Button
            Button(
                onClick = {
                    errorMessage = null
                    showGoogleOAuthSheet = true
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimaryDark,
                    contentColor = BackgroundDark
                )
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDark)
                Text("ATAU EMAIL", color = TextMutedDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDark)
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email address", fontSize = 12.sp, color = TextMutedDark) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", fontSize = 12.sp, color = TextMutedDark) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        CloudApi.loginWithCredentials(email, password)
                            .onSuccess { resp ->
                                if (resp.mfaRequired || resp.data?.mfaRequired == true) {
                                    onMfaRequired(resp.data?.mfaToken ?: "", email, password, false)
                                } else {
                                    onSuccess()
                                }
                            }
                            .onFailure { err ->
                                errorMessage = err.message ?: "Gagal masuk"
                            }
                        isLoading = false
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TextPrimaryDark)
                } else {
                    Text("Sign In", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showGoogleOAuthSheet) {
        Dialog(
            onDismissRequest = { showGoogleOAuthSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                color = SurfaceDark
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevatedDark)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sign in with Google", color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showGoogleOAuthSheet = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                        }
                    }
                    PlatformOAuthWebView(
                        authUrl = "${CloudApi.BASE_URL}/auth/google?prompt=select_account",
                        onTicketReceived = { ticket ->
                            showGoogleOAuthSheet = false
                            scope.launch {
                                isLoading = true
                                CloudApi.exchangeGoogleTicket(ticket)
                                    .onSuccess { onSuccess() }
                                    .onFailure { errorMessage = it.message }
                                isLoading = false
                            }
                        },
                        onMfaRequired = { mfaToken, mfaEmail ->
                            showGoogleOAuthSheet = false
                            onMfaRequired(mfaToken, mfaEmail, "", true)
                        },
                        onError = { msg ->
                            showGoogleOAuthSheet = false
                            errorMessage = msg
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun IosTotpVerificationScreen(
    email: String,
    mfaToken: String,
    password: String,
    isGoogleMfa: Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Welcome back", color = TextPrimaryDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Verifikasi keamanan akun Anda ($email)", color = TextSecondaryDark, fontSize = 12.sp, textAlign = TextAlign.Center)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevatedDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                    Text("Verifikasi Dua Langkah", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Masukkan 6-digit kode dari Google Authenticator atau salah satu dari 10 Kode Cadangan Anda (contoh: 58B5-A215).",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg!!, color = AccentRed, fontSize = 11.sp)
            }

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text("123456 ATAU 58B5-A215", fontSize = 13.sp, color = TextMutedDark) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMsg = null
                        val clean = code.trim().lines().firstOrNull()?.trim() ?: code.trim()
                        if (isGoogleMfa) {
                            CloudApi.verifyGoogleMfa(mfaToken, clean)
                                .onSuccess { onSuccess() }
                                .onFailure { errorMsg = it.message }
                        } else {
                            CloudApi.loginWithCredentials(email, password, clean)
                                .onSuccess { onSuccess() }
                                .onFailure { errorMsg = it.message }
                        }
                        isLoading = false
                    }
                },
                enabled = code.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TextPrimaryDark)
                } else {
                    Text("Verifikasi & Masuk", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(onClick = onCancel) {
                Text("Batalkan dan kembali ke menu login", color = TextSecondaryDark, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosMainWorkspaceScreen(
    onOpenSettings: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(BottomTab.DRIVE) }

    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var breadcrumbs by remember { mutableStateOf<List<BreadcrumbDto>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FolderItemDto>>(emptyList()) }
    var files by remember { mutableStateOf<List<FileItemDto>>(emptyList()) }
    var quota by remember { mutableStateOf(QuotaResponseDto()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Multi-select state
    var selectedFileIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedFileIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var shareTarget by remember { mutableStateOf<ShareDialogTarget?>(null) }
    var previewFile by remember { mutableStateOf<FileItemDto?>(null) }

    fun clearSelection() {
        selectedFileIds = emptySet()
        selectedFolderIds = emptySet()
    }

    fun selectAll() {
        selectedFolderIds = folders.map { it.id }.toSet()
        selectedFileIds = files.map { it.id }.toSet()
    }

    fun refreshData() {
        scope.launch {
            isLoading = true
            when (activeTab) {
                BottomTab.DRIVE -> {
                    folders = CloudApi.listFolders(currentFolderId)
                    files = CloudApi.listFiles(currentFolderId)
                    breadcrumbs = if (currentFolderId != null) CloudApi.getBreadcrumbs(currentFolderId!!) else emptyList()
                    quota = CloudApi.getQuota()
                }
                BottomTab.SHARED -> {
                    val shared = CloudApi.getSharedWithMe()
                    folders = shared.folders
                    files = shared.files
                }
                BottomTab.TRASH -> {
                    val trash = CloudApi.getTrash()
                    folders = trash.folders
                    files = trash.files
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(activeTab, currentFolderId) {
        clearSelection()
        refreshData()
    }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevatedDark)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { clearSelection() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextPrimaryDark)
                        }
                        Text(
                            text = "${selectedFileIds.size + selectedFolderIds.size} dipilih",
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val allSelected = (selectedFileIds.size + selectedFolderIds.size) == (folders.size + files.size)
                        IconButton(
                            onClick = { if (allSelected) clearSelection() else selectAll() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = if (allSelected) PrimaryBlue else TextPrimaryDark)
                        }
                        IconButton(
                            onClick = {
                                val count = selectedFileIds.size + selectedFolderIds.size
                                shareTarget = ShareDialogTarget(
                                    title = "Share \"$count items\"",
                                    isBatch = true,
                                    batchFileIds = selectedFileIds.toList(),
                                    batchFolderIds = selectedFolderIds.toList()
                                )
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Selected", tint = PrimaryBlue)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    CloudApi.batchDelete(selectedFileIds.toList(), selectedFolderIds.toList())
                                        .onSuccess {
                                            onMessage("Item dipindahkan ke sampah")
                                            clearSelection()
                                            refreshData()
                                        }
                                        .onFailure { onMessage(it.message ?: "Gagal menghapus") }
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = AccentRed)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                            Text("Zacode Cloud", color = TextPrimaryDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (folders.isNotEmpty() || files.isNotEmpty()) {
                                IconButton(onClick = { selectAll() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = TextSecondaryDark, modifier = Modifier.size(19.dp))
                                }
                            }
                            IconButton(onClick = { refreshData() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondaryDark, modifier = Modifier.size(19.dp))
                            }
                            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile & Security", tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari file atau folder...", fontSize = 12.sp, color = TextMutedDark) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(17.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == BottomTab.DRIVE) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FloatingActionButton(
                        onClick = {
                            newFolderName = ""
                            showCreateFolderDialog = true
                        },
                        containerColor = SurfaceElevatedDark,
                        contentColor = TextPrimaryDark,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Folder Baru", modifier = Modifier.size(20.dp))
                    }
                    FloatingActionButton(
                        onClick = {
                            PlatformBridge.pickFilesForUpload { pickedList ->
                                if (pickedList.isNotEmpty()) {
                                    scope.launch {
                                        onMessage("Mengunggah ${pickedList.size} file...")
                                        var successCount = 0
                                        pickedList.forEach { pf ->
                                            CloudApi.uploadFile(pf, currentFolderId).onSuccess { successCount++ }
                                        }
                                        onMessage("$successCount file berhasil diunggah")
                                        refreshData()
                                    }
                                }
                            }
                        },
                        containerColor = PrimaryBlue,
                        contentColor = TextPrimaryDark,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload File", modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = activeTab == BottomTab.DRIVE,
                    onClick = { activeTab = BottomTab.DRIVE },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(19.dp)) },
                    label = { Text("My Drive", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == BottomTab.SHARED,
                    onClick = { activeTab = BottomTab.SHARED },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(19.dp)) },
                    label = { Text("Shared", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == BottomTab.TRASH,
                    onClick = { activeTab = BottomTab.TRASH },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(19.dp)) },
                    label = { Text("Trash", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            // Breadcrumbs in Drive tab
            if (activeTab == BottomTab.DRIVE) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "My Drive",
                            color = if (currentFolderId == null) PrimaryBlue else TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { currentFolderId = null }
                        )
                    }
                    items(breadcrumbs) { crumb ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("/", color = TextMutedDark, fontSize = 12.sp)
                            Text(
                                text = crumb.name,
                                color = if (crumb.id == currentFolderId) PrimaryBlue else TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { currentFolderId = crumb.id }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 84.dp)
            ) {
                if (filteredFolders.isEmpty() && filteredFiles.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(40.dp))
                                Text("Belum ada file atau folder", color = TextSecondaryDark, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Folders
                items(filteredFolders, key = { "folder_${it.id}" }) { folder ->
                    val isSelected = selectedFolderIds.contains(folder.id)
                    var menuOpen by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.16f) else SurfaceDark)
                            .border(1.dp, if (isSelected) PrimaryBlue else BorderDark, RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedFolderIds = if (isSelected) selectedFolderIds - folder.id else selectedFolderIds + folder.id
                                    } else if (activeTab == BottomTab.DRIVE) {
                                        currentFolderId = folder.id
                                    }
                                },
                                onLongClick = {
                                    selectedFolderIds = if (isSelected) selectedFolderIds - folder.id else selectedFolderIds + folder.id
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Folder,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = folder.name,
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondaryDark, modifier = Modifier.size(17.dp))
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                modifier = Modifier.background(SurfaceElevatedDark)
                            ) {
                                if (activeTab != BottomTab.TRASH) {
                                    DropdownMenuItem(
                                        text = { Text("Share", color = TextPrimaryDark, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            shareTarget = ShareDialogTarget(
                                                title = "Share \"${folder.name}\"",
                                                isBatch = false,
                                                resourceType = "folder",
                                                resourceId = folder.id
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename", color = TextPrimaryDark, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            renameValue = folder.name
                                            renameTarget = folder.id to true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = AccentRed, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            scope.launch {
                                                CloudApi.deleteItem(folder.id, true)
                                                refreshData()
                                            }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Restore", color = AccentEmerald, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            scope.launch {
                                                CloudApi.restoreFromTrash(folder.id, true)
                                                refreshData()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Files
                items(filteredFiles, key = { "file_${it.id}" }) { file ->
                    val isSelected = selectedFileIds.contains(file.id)
                    var menuOpen by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.16f) else SurfaceDark)
                            .border(1.dp, if (isSelected) PrimaryBlue else BorderDark, RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedFileIds = if (isSelected) selectedFileIds - file.id else selectedFileIds + file.id
                                    } else {
                                        previewFile = file
                                    }
                                },
                                onLongClick = {
                                    selectedFileIds = if (isSelected) selectedFileIds - file.id else selectedFileIds + file.id
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryBlue else AccentEmerald,
                                modifier = Modifier.size(21.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatBytes(file.size),
                                    color = TextMutedDark,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondaryDark, modifier = Modifier.size(17.dp))
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                modifier = Modifier.background(SurfaceElevatedDark)
                            ) {
                                if (activeTab != BottomTab.TRASH) {
                                    DropdownMenuItem(
                                        text = { Text("Preview / Download", color = TextPrimaryDark, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            previewFile = file
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share", color = TextPrimaryDark, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            shareTarget = ShareDialogTarget(
                                                title = "Share \"${file.name}\"",
                                                isBatch = false,
                                                resourceType = "file",
                                                resourceId = file.id
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename", color = TextPrimaryDark, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            renameValue = file.name
                                            renameTarget = file.id to false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = AccentRed, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            scope.launch {
                                                CloudApi.deleteItem(file.id, false)
                                                refreshData()
                                            }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Restore", color = AccentEmerald, fontSize = 12.sp) },
                                        onClick = {
                                            menuOpen = false
                                            scope.launch {
                                                CloudApi.restoreFromTrash(file.id, false)
                                                refreshData()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Folder Baru", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Nama folder...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            CloudApi.createFolder(newFolderName, currentFolderId)
                                .onSuccess {
                                    showCreateFolderDialog = false
                                    refreshData()
                                }
                                .onFailure { onMessage(it.message ?: "Gagal membuat folder") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Buat", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        )
    }

    // Rename Dialog
    if (renameTarget != null) {
        val (targetId, isFolder) = renameTarget!!
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = SurfaceDark,
            title = { Text("Ubah Nama", color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            CloudApi.renameItem(targetId, isFolder, renameValue)
                                .onSuccess {
                                    renameTarget = null
                                    refreshData()
                                }
                                .onFailure { onMessage(it.message ?: "Gagal mengubah nama") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        )
    }

    // Share Dialog
    if (shareTarget != null) {
        val target = shareTarget!!
        IosShareDialog(
            title = target.title,
            isBatch = target.isBatch,
            resourceType = target.resourceType,
            resourceId = target.resourceId,
            batchFileIds = target.batchFileIds,
            batchFolderIds = target.batchFolderIds,
            onDismiss = {
                shareTarget = null
                if (target.isBatch) clearSelection()
            },
            onMessage = onMessage
        )
    }

    // File Preview / Download Dialog
    if (previewFile != null) {
        val f = previewFile!!
        AlertDialog(
            onDismissRequest = { previewFile = null },
            containerColor = SurfaceDark,
            title = { Text(f.name, color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ukuran: ${formatBytes(f.size)}", color = TextSecondaryDark, fontSize = 12.sp)
                    Text("Tipe: ${f.mimeType}", color = TextMutedDark, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            CloudApi.getPreviewUrl(f.id)
                                .onSuccess { url ->
                                    previewFile = null
                                    PlatformBridge.openExternalUrl(url)
                                }
                                .onFailure { onMessage(it.message ?: "Gagal membuka file") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Buka / Unduh di iOS", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewFile = null }) {
                    Text("Tutup", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        )
    }
}

