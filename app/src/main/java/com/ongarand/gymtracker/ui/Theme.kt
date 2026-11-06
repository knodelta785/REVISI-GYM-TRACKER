package com.ongarand.gymtracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OngarandColors = darkColorScheme(
    primary = Color(0xFFFF6D00),
    onPrimary = Color(0xFF1A0E00),
    primaryContainer = Color(0xFF4A2200),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFF2EE6A6),
    onSecondary = Color(0xFF00241A),
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF181B21),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF262B33),
    onSurfaceVariant = Color(0xFFA9B1BD),
    surfaceContainer = Color(0xFF181B21),
    outline = Color(0xFF3A414C),
    error = Color(0xFFFF5370)
)

@Composable
fun OngarandTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OngarandColors, content = content)
}
