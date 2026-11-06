package com.cloudstorage.ios.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class PickedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray
)

expect object PlatformBridge {
    fun getString(key: String): String?
    fun setString(key: String, value: String?)
    fun clearStorage()
    fun copyToClipboard(text: String)
    fun openExternalUrl(url: String)
    fun pickFilesForUpload(onResult: (List<PickedFile>) -> Unit)
}

@Composable
expect fun PlatformOAuthWebView(
    authUrl: String,
    onTicketReceived: (ticket: String) -> Unit,
    onMfaRequired: (mfaToken: String, email: String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
)

