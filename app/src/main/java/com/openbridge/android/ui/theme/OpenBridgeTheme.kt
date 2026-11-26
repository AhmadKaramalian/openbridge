package com.openbridge.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// OpenBridge brand colors (matches colors.xml)
private val Primary = Color(0xFF1A56DB)
private val PrimaryDark = Color(0xFF0F3D91)
private val Accent = Color(0xFF22C55E)
private val BgLight = Color(0xFFFAFBFC)
private val BgDark = Color(0xFF0F172A)
private val TextLight = Color(0xFF0F172A)
private val TextDark = Color(0xFFE2E8F0)
private val MutedLight = Color(0xFF64748B)
private val MutedDark = Color(0xFF94A3B8)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = TextLight,
    surface = Color.White,
    onSurface = TextLight,
    onSurfaceVariant = MutedLight,
    error = Color(0xFFDC2626)
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    background = BgDark,
    onBackground = TextDark,
    surface = Color(0xFF1E293B),
    onSurface = TextDark,
    onSurfaceVariant = MutedDark,
    error = Color(0xFFEF4444)
)

@Composable
fun OpenBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
