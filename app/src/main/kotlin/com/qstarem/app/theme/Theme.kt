package com.qstarem.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QStaremDarkColors = darkColorScheme(
    primary = Color(0xFF9B5DE5),
    onPrimary = Color(0xFF0A0A0F),
    secondary = Color(0xFFFF6B9D),
    onSecondary = Color(0xFF0A0A0F),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFF5F5FA),
    surface = Color(0xFF12121A),
    onSurface = Color(0xFFF5F5FA),
    surfaceVariant = Color(0xFF1A1A26),
    onSurfaceVariant = Color(0xFFB8B8C8),
    outline = Color(0xFF3A3A4A),
)

@Composable
fun QStaremTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QStaremDarkColors,
        content = content,
    )
}
