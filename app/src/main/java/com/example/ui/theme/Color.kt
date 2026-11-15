package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Midnight Black & Electric Purple Core Palette
val MidnightBlack = Color(0xFF07060B)
val ObsidianSurface = Color(0xFF100D1A)
val ObsidianElevated = Color(0xFF1A1529)
val GlassCardDark = Color(0xFF211A36)
val GlassBorderDark = Color(0xFF342A54)

val ElectricPurple = Color(0xFF9D4EDD)
val NeonViolet = Color(0xFFC77DFF)
val DeepRoyalPurple = Color(0xFF5A189A)
val CyberCyan = Color(0xFF00F5D4)
val HotMagenta = Color(0xFFFF4D8D)
val SolarGold = Color(0xFFFFB703)
val EmeraldCloseFriend = Color(0xFF10B981)

val TextPrimaryDark = Color(0xFFF6F4FF)
val TextSecondaryDark = Color(0xFFB3A9D1)
val TextMutedDark = Color(0xFF776D96)

// Light Mode Palette
val PearlBackground = Color(0xFFF7F5FC)
val LavenderSurface = Color(0xFFFFFFFF)
val LavenderContainer = Color(0xFFEEE8FA)
val LavenderBorder = Color(0xFFD9CCF2)
val DeepIndigoText = Color(0xFF160E26)
val MutedVioletText = Color(0xFF5D4F7A)
val LightElectricPurple = Color(0xFF7B2CBF)

val VibeError = Color(0xFFFF5A79)

data class VibeCardStyle(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color
)

val VibeCardThemes = listOf(
    VibeCardStyle("electric_purple", "Electric Purple", Color(0xFF5A189A), Color(0xFF9D4EDD), Color(0xFF00F5D4)),
    VibeCardStyle("cyber_cyan", "Cyber Cyan", Color(0xFF005F73), Color(0xFF0A9396), Color(0xFF00F5D4)),
    VibeCardStyle("neon_rose", "Neon Rose", Color(0xFF800F2F), Color(0xFFC9184A), Color(0xFFFF758F)),
    VibeCardStyle("solar_gold", "Solar Gold", Color(0xFF7F4F24), Color(0xFFCA6702), Color(0xFFFFB703)),
    VibeCardStyle("obsidian_glass", "Obsidian Glass", Color(0xFF100D1A), Color(0xFF2B2147), Color(0xFFC77DFF))
)

data class ChatThemeStyle(
    val id: String,
    val label: String,
    val bubbleOwnStart: Color,
    val bubbleOwnEnd: Color,
    val backdropTint: Color
)

val VibeChatThemes = listOf(
    ChatThemeStyle("midnight_purple", "Midnight Purple", Color(0xFF7B2CBF), Color(0xFF9D4EDD), Color(0xFF0D0917)),
    ChatThemeStyle("neon_cyan", "Neon Cyan", Color(0xFF0077B6), Color(0xFF00B4D8), Color(0xFF06131C)),
    ChatThemeStyle("velvet_rose", "Velvet Rose", Color(0xFFA4133C), Color(0xFFFF4D8D), Color(0xFF180811)),
    ChatThemeStyle("emerald_matrix", "Emerald Pulse", Color(0xFF047857), Color(0xFF10B981), Color(0xFF061611))
)

fun cardThemeForId(id: String): VibeCardStyle =
    VibeCardThemes.firstOrNull { it.id == id } ?: VibeCardThemes.first()

fun chatThemeForId(id: String): ChatThemeStyle =
    VibeChatThemes.firstOrNull { it.id == id } ?: VibeChatThemes.first()
