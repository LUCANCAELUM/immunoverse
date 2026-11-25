package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ImmunoDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidMidnight,
    primaryContainer = Color(0xFF0A3A52),
    onPrimaryContainer = IceWhite,
    secondary = ScientificViolet,
    onSecondary = Color(0xFF09051A),
    secondaryContainer = Color(0xFF281F54),
    onSecondaryContainer = IceWhite,
    tertiary = BioTurquoise,
    onTertiary = VoidMidnight,
    tertiaryContainer = Color(0xFF0D3D38),
    onTertiaryContainer = IceWhite,
    background = VoidMidnight,
    onBackground = IceWhite,
    surface = DeepNavy,
    onSurface = IceWhite,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = VoidMidnight,
    surfaceContainerLow = DeepNavy,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = SurfaceHighlight,
    surfaceContainerHighest = Color(0xFF1D3161),
    outline = GlassBorderColor,
    outlineVariant = Color(0x2278C8FF),
    error = CriticalRose,
    onError = VoidMidnight,
    errorContainer = Color(0xFF47121E),
    onErrorContainer = Color(0xFFFFD9DF)
)

val ImmunoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun ImmunoVerseTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalImmunoColors provides ImmunoSemanticColors()) {
        MaterialTheme(
            colorScheme = ImmunoDarkColorScheme,
            typography = ImmunoTypography,
            shapes = ImmunoShapes,
            content = content
        )
    }
}
