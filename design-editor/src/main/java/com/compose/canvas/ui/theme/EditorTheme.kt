package com.compose.canvas.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Fixed high-contrast chrome for the editor.
 * Dynamic/dark Material You on Samsung often yields black icons on black panels.
 */
val EditorLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4F378B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF21005E),
    secondary = Color(0xFF006C4C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF8FF7C8),
    onSecondaryContainer = Color(0xFF002114),
    tertiary = Color(0xFF00658E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFE8E8E8),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF7F7FA),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF3F3F7),
    inversePrimary = Color(0xFFD0BCFF)
)

val ChromeSurface = Color(0xFFF7F7FA)
val ChromeOnSurface = Color(0xFF1B1B1F)
val ChromeBorder = Color(0xFFC7C5D0)
val ChromeIcon = Color(0xFF1B1B1F)
val ChromeIconMuted = Color(0xFF5C5B64)
val ChromeIconDisabled = Color(0xFF8B8A93)

@Composable
fun EditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EditorLightScheme, content = content)
}
