package com.cloudstorage.ios.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BackgroundDark = Color(0xFF09090B)
val SurfaceDark = Color(0xFF121215)
val SurfaceElevatedDark = Color(0xFF18181B)
val BorderDark = Color(0xFF27272A)
val TextPrimaryDark = Color(0xFFFAFAFA)
val TextSecondaryDark = Color(0xFFA1A1AA)
val TextMutedDark = Color(0xFF71717A)
val PrimaryBlue = Color(0xFF3B82F6)
val AccentEmerald = Color(0xFF10B981)
val AccentRed = Color(0xFFEF4444)
val AccentAmber = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentEmerald,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevatedDark,
    onPrimary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = AccentRed
)

private val CompactTypography = Typography(
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
)

@Composable
fun ZacodeIosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CompactTypography,
        content = content
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    val rounded = ((value * 10.0).toLong() / 10.0)
    return "$rounded ${units[unitIndex]}"
}

