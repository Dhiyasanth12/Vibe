package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VibeDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = DeepRoyalPurple,
    onPrimaryContainer = TextPrimaryDark,
    secondary = CyberCyan,
    onSecondary = MidnightBlack,
    secondaryContainer = GlassCardDark,
    onSecondaryContainer = CyberCyan,
    tertiary = HotMagenta,
    onTertiary = Color.White,
    background = MidnightBlack,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassBorderDark,
    error = VibeError,
    onError = Color.White
)

private val VibeLightColorScheme = lightColorScheme(
    primary = LightElectricPurple,
    onPrimary = Color.White,
    primaryContainer = LavenderContainer,
    onPrimaryContainer = DeepIndigoText,
    secondary = Color(0xFF008B8B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7F5),
    onSecondaryContainer = Color(0xFF004D4D),
    tertiary = HotMagenta,
    onTertiary = Color.White,
    background = PearlBackground,
    onBackground = DeepIndigoText,
    surface = LavenderSurface,
    onSurface = DeepIndigoText,
    surfaceVariant = LavenderContainer,
    onSurfaceVariant = MutedVioletText,
    outline = LavenderBorder,
    error = VibeError,
    onError = Color.White
)

val VibeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun VibeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VibeDarkColorScheme else VibeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VibeTypography,
        shapes = VibeShapes,
        content = content
    )
}
