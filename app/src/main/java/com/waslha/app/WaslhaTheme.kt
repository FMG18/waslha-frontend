package com.waslha.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val WaslhaPrimary = Color(0xFF0A8F68)
private val WaslhaPrimaryContainer = Color(0xFFD7F5E9)
private val WaslhaOnPrimaryContainer = Color(0xFF063D2C)
private val WaslhaSecondary = Color(0xFF6F4DBA)
private val WaslhaSecondaryContainer = Color(0xFFEAE1FF)
private val WaslhaSurface = Color(0xFFFFFFFF)
private val WaslhaBackground = Color(0xFFF5F8F6)
private val WaslhaOnSurface = Color(0xFF10201B)
private val WaslhaOnSurfaceVariant = Color(0xFF66736E)
private val WaslhaOutline = Color(0xFFD5DFDA)

private val LightWaslhaColors = lightColorScheme(
    primary = WaslhaPrimary,
    primaryContainer = WaslhaPrimaryContainer,
    onPrimaryContainer = WaslhaOnPrimaryContainer,
    secondary = WaslhaSecondary,
    secondaryContainer = WaslhaSecondaryContainer,
    surface = WaslhaSurface,
    background = WaslhaBackground,
    onSurface = WaslhaOnSurface,
    onSurfaceVariant = WaslhaOnSurfaceVariant,
    outline = WaslhaOutline
)

@Suppress("unused")
private val DarkWaslhaColors = darkColorScheme(
    primary = Color(0xFF4FD3A3),
    onPrimary = Color(0xFF003828),
    secondary = Color(0xFFD4BBFF),
    surface = Color(0xFF111814),
    background = Color(0xFF0D120F),
    onSurface = Color(0xFFE7EEE9),
    onSurfaceVariant = Color(0xFFB7C4BD),
    outline = Color(0xFF4A5851)
)

private val WaslhaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WaslhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightWaslhaColors,
        shapes = WaslhaShapes,
        typography = Typography(),
        content = content
    )
}
