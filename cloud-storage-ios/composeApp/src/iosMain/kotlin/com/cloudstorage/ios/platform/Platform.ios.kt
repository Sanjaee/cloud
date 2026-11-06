@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.cloudstorage.ios.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.delay
import platform.Foundation.*
import platform.UIKit.*
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.posix.memcpy

private var activeDocumentDelegate: NSObject? = null

actual object PlatformBridge {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun setString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
        defaults.synchronize()
    }

    actual fun clearStorage() {
        listOf(
            "zacode_access_token",
            "zacode_refresh_token",
            "zacode_user_email",
            "zacode_username"
        ).forEach { defaults.removeObjectForKey(it) }
        defaults.synchronize()
    }

    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun openExternalUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(
            nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }

    actual fun pickFilesForUpload(onResult: (List<PickedFile>) -> Unit) {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.item", "public.data", "public.content"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
        )
        picker.allowsMultipleSelection = true

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val results = mutableListOf<PickedFile>()
                didPickDocumentsAtURLs.filterIsInstance<NSURL>().forEach { url ->
                    val name = url.lastPathComponent ?: "uploaded_file"
                    val data = NSData.dataWithContentsOfURL(url)
                    if (data != null) {
                        val bytes = data.toByteArray()
                        results.add(
                            PickedFile(
                                name = name,
                                mimeType = guessMimeType(name),
                                bytes = bytes
                            )
                        )
                    }
                }
                activeDocumentDelegate = null
                onResult(results)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                activeDocumentDelegate = null
                onResult(emptyList())
            }
        }

        activeDocumentDelegate = delegate
        picker.delegate = delegate
        rootController.presentViewController(picker, animated = true, completion = null)
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size <= 0) return ByteArray(0)
    val bytes = ByteArray(size)
    val src = this.bytes
    if (src != null) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), src, this.length)
        }
    }
    return bytes
}

private fun guessMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "txt" -> "text/plain"
        "json" -> "application/json"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }
}

private fun extractQueryParam(url: String, param: String): String? {
    val query = url.substringAfter('?', "")
    if (query.isEmpty()) return null
    val pairs = query.substringBefore('#').split('&')
    for (pair in pairs) {
        val parts = pair.split('=', limit = 2)
        if (parts.size == 2 && parts[0] == param) {
            return NSString.create(string = parts[1]).stringByRemovingPercentEncoding ?: parts[1]
        }
    }
    return null
}

@Composable
actual fun PlatformOAuthWebView(
    authUrl: String,
    onTicketReceived: (ticket: String) -> Unit,
    onMfaRequired: (mfaToken: String, email: String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier
) {
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }
    var handled by remember { mutableStateOf(false) }

    LaunchedEffect(webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        while (!handled) {
            delay(200)
            val currentUrl = wv.URL?.absoluteString ?: continue
            val ticket = extractQueryParam(currentUrl, "ticket")
            if (!ticket.isNullOrBlank()) {
                handled = true
                wv.stopLoading()
                onTicketReceived(ticket)
                break
            }
            val mfaToken = extractQueryParam(currentUrl, "mfa_token")
            if (!mfaToken.isNullOrBlank()) {
                handled = true
                val email = extractQueryParam(currentUrl, "email") ?: ""
                wv.stopLoading()
                onMfaRequired(mfaToken, email)
                break
            }
            val error = extractQueryParam(currentUrl, "error")
            if (!error.isNullOrBlank() && currentUrl.contains("/login")) {
                handled = true
                wv.stopLoading()
                onError(error)
                break
            }
        }
    }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            val wv = WKWebView(frame = platform.CoreGraphics.CGRectZero.readValue(), configuration = config)
            wv.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
            val nsUrl = NSURL.URLWithString(authUrl)
            if (nsUrl != null) {
                wv.loadRequest(NSURLRequest.requestWithURL(nsUrl))
            }
            webViewRef = wv
            wv
        },
        modifier = modifier
    )
}

