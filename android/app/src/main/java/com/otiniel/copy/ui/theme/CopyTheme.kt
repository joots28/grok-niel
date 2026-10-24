package com.otiniel.copy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CopyBackground = Color(0xFF080A0F)
val CopySurface = Color(0xFF11141B)
val CopySurfaceRaised = Color(0xFF191D27)
val CopyPrimary = Color(0xFF9B8CFF)
val CopySecondary = Color(0xFF62D8E8)
val CopyText = Color(0xFFF2F3F8)
val CopyMuted = Color(0xFF9299AA)
val CopyBorder = Color(0xFF282E3B)
val CopySuccess = Color(0xFF70D6A1)
val CopyWarning = Color(0xFFFFC777)

private val CopyDarkColors = darkColorScheme(
    primary = CopyPrimary,
    onPrimary = Color(0xFF17132A),
    secondary = CopySecondary,
    background = CopyBackground,
    onBackground = CopyText,
    surface = CopySurface,
    onSurface = CopyText,
    surfaceVariant = CopySurfaceRaised,
    onSurfaceVariant = CopyMuted,
    outline = CopyBorder,
    error = Color(0xFFFF7B86),
)

@Composable
fun CopyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CopyDarkColors,
        content = content,
    )
}
