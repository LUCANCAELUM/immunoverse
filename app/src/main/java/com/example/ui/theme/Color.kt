package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Futuristic Midnight Scientific Palette
val VoidMidnight = Color(0xFF050914)
val DeepNavy = Color(0xFF0A1226)
val ElevatedSurface = Color(0xFF101C3B)
val SurfaceHighlight = Color(0xFF16264E)
val ElectricCyan = Color(0xFF22E5FF)
val ScientificViolet = Color(0xFF8B6BFF)
val BioTurquoise = Color(0xFF2EE6C5)
val SubtleMagenta = Color(0xFFE04FB0)

val IceWhite = Color(0xFFE6F1FF)
val MutedSlate = Color(0xFF8DA2C4)
val GlassBorderColor = Color(0x3878C8FF)
val GlassPanelColor = Color(0xB80E1935)

val EstablishedGreen = Color(0xFF3DDC97)
val IllustrativeAmber = Color(0xFFFFC857)
val CriticalRose = Color(0xFFFF5D73)

@Immutable
data class ImmunoSemanticColors(
    val glassBg: Color = GlassPanelColor,
    val glassBorder: Color = GlassBorderColor,
    val established: Color = EstablishedGreen,
    val illustrative: Color = IllustrativeAmber,
    val hypothesis: Color = SubtleMagenta,
    val simulated: Color = ScientificViolet,
    val turquoise: Color = BioTurquoise,
    val mutedText: Color = MutedSlate,
    val deepPanel: Color = DeepNavy,
    val critical: Color = CriticalRose
)

val LocalImmunoColors = staticCompositionLocalOf { ImmunoSemanticColors() }
