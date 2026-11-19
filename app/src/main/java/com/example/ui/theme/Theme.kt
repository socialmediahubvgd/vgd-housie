package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VgdColorScheme = darkColorScheme(
    primary = CyanDiamond,
    onPrimary = TextDark,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanDiamondBright,
    secondary = GoldCoin,
    onSecondary = TextDark,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldCoinBright,
    tertiary = EmeraldSuccess,
    onTertiary = TextDark,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldSuccess,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline,
    error = CoralError,
    onError = TextDark,
    errorContainer = CoralContainer,
    onErrorContainer = CoralError
)

private val VgdShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VgdHousieTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VgdColorScheme,
        typography = VgdTypography,
        shapes = VgdShapes,
        content = content
    )
}
