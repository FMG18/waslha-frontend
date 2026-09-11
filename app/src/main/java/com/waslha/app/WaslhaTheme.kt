package com.waslha.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WaslhaEmerald = Color(0xFF087F5B)
private val WaslhaEmeraldDark = Color(0xFF055C42)
private val WaslhaEmeraldSoft = Color(0xFFE7F6F0)
private val WaslhaLime = Color(0xFFB8E986)
private val WaslhaLimeSoft = Color(0xFFEDF8DE)
private val WaslhaPurple = Color(0xFF6F4DBA)
private val WaslhaPurpleSoft = Color(0xFFF0EAFE)
private val WaslhaInk = Color(0xFF12201B)
private val WaslhaMuted = Color(0xFF66756F)
private val WaslhaSurface = Color(0xFFFFFFFF)
private val WaslhaSurfaceAlt = Color(0xFFF8FAF9)
private val WaslhaBackground = Color(0xFFF3F6F4)
private val WaslhaOutline = Color(0xFFDCE5E1)
private val WaslhaError = Color(0xFFB42318)
private val WaslhaErrorSoft = Color(0xFFFFF1EF)

private val LightWaslhaColors = lightColorScheme(
    primary = WaslhaEmerald,
    onPrimary = Color.White,
    primaryContainer = WaslhaEmeraldSoft,
    onPrimaryContainer = WaslhaEmeraldDark,
    secondary = WaslhaLime,
    onSecondary = WaslhaInk,
    secondaryContainer = WaslhaLimeSoft,
    onSecondaryContainer = WaslhaInk,
    tertiary = WaslhaPurple,
    onTertiary = Color.White,
    tertiaryContainer = WaslhaPurpleSoft,
    onTertiaryContainer = Color(0xFF34205F),
    surface = WaslhaSurface,
    surfaceVariant = WaslhaSurfaceAlt,
    background = WaslhaBackground,
    onSurface = WaslhaInk,
    onSurfaceVariant = WaslhaMuted,
    outline = WaslhaOutline,
    error = WaslhaError,
    errorContainer = WaslhaErrorSoft,
    onErrorContainer = Color(0xFF7D1A12)
)

private val DarkWaslhaColors = darkColorScheme(
    primary = Color(0xFF67D7B2),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF07543F),
    onPrimaryContainer = Color(0xFFB7F2DC),
    secondary = Color(0xFFCBEF9E),
    onSecondary = Color(0xFF263A15),
    secondaryContainer = Color(0xFF3A5420),
    onSecondaryContainer = Color(0xFFE0F6B7),
    tertiary = Color(0xFFD3BDFF),
    onTertiary = Color(0xFF382064),
    tertiaryContainer = Color(0xFF51377A),
    onTertiaryContainer = Color(0xFFEBDFFF),
    surface = Color(0xFF101613),
    surfaceVariant = Color(0xFF1A211E),
    background = Color(0xFF0B100E),
    onSurface = Color(0xFFE7EEE9),
    onSurfaceVariant = Color(0xFFB8C4BE),
    outline = Color(0xFF45524D),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val WaslhaShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val WaslhaTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 47.sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 41.sp),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 37.sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 35.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 31.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = 21.sp, lineHeight = 28.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
        bodySmall = bodySmall.copy(fontSize = 11.sp, lineHeight = 17.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp)
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
