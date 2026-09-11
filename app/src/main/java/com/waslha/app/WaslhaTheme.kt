package com.waslha.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val WaslhaEmerald = Color(0xFF087F5B)
private val WaslhaEmeraldDark = Color(0xFF055C42)
private val WaslhaMint = Color(0xFFE7F6F0)
private val WaslhaLime = Color(0xFFB8E986)
private val WaslhaInk = Color(0xFF12201B)
private val WaslhaMuted = Color(0xFF6D7A75)
private val WaslhaSurface = Color(0xFFFFFFFF)
private val WaslhaBackground = Color(0xFFF7F9F8)
private val WaslhaOutline = Color(0xFFDDE5E1)
private val WaslhaError = Color(0xFFB42318)

private val LightWaslhaColors = lightColorScheme(
    primary = WaslhaEmerald,
    onPrimary = Color.White,
    primaryContainer = WaslhaMint,
    onPrimaryContainer = WaslhaEmeraldDark,
    secondary = WaslhaLime,
    onSecondary = WaslhaInk,
    secondaryContainer = Color(0xFFEAF7D9),
    onSecondaryContainer = WaslhaInk,
    tertiary = Color(0xFF6F4DBA),
    surface = WaslhaSurface,
    background = WaslhaBackground,
    onSurface = WaslhaInk,
    onSurfaceVariant = WaslhaMuted,
    outline = WaslhaOutline,
    error = WaslhaError
)

private val DarkWaslhaColors = darkColorScheme(
    primary = Color(0xFF67D7B2),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF07543F),
    onPrimaryContainer = Color(0xFFB7F2DC),
    secondary = Color(0xFFCBEF9E),
    onSecondary = Color(0xFF263A15),
    tertiary = Color(0xFFD3BDFF),
    surface = Color(0xFF111714),
    background = Color(0xFF0B100E),
    onSurface = Color(0xFFE7EEE9),
    onSurfaceVariant = Color(0xFFB8C4BE),
    outline = Color(0xFF45524D),
    error = Color(0xFFFFB4AB)
)

private val WaslhaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val WaslhaTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 40.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 31.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp)
    )
}

@Composable
fun WaslhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightWaslhaColors,
        shapes = WaslhaShapes,
        typography = WaslhaTypography,
        content = content
    )
}
