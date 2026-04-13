package com.bolsaaf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material Design 3 Color Tokens
// Seed Color: Primary Orange from BolSaaf Brand
private val SeedColor = Color(0xFFFF6B35)

// Dark Theme Colors (Material 3 - Dynamic)
private val DarkPrimary = Color(0xFFFFB5A3)
private val DarkOnPrimary = Color(0xFF661E0C)
private val DarkPrimaryContainer = Color(0xFF882415)
private val DarkOnPrimaryContainer = Color(0xFFFFDCD1)

private val DarkSecondary = Color(0xFFE5B9A6)
private val DarkOnSecondary = Color(0xFF4A2C1F)
private val DarkSecondaryContainer = Color(0xFF633C31)
private val DarkOnSecondaryContainer = Color(0xFFFEDAC4)

private val DarkTertiary = Color(0xFFFFB59A)
private val DarkOnTertiary = Color(0xFF5D2C0F)
private val DarkTertiaryContainer = Color(0xFF7E4018)
private val DarkOnTertiaryContainer = Color(0xFFFFDCC7)

private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)

private val DarkBackground = Color(0xFF201A19)
private val DarkOnBackground = Color(0xFFEBE1DE)
private val DarkSurface = Color(0xFF201A19)
private val DarkOnSurface = Color(0xFFEBE1DE)
private val DarkSurfaceVariant = Color(0xFF52443E)
private val DarkOnSurfaceVariant = Color(0xFFD8C6BE)
private val DarkOutline = Color(0xFFA09088)
private val DarkOutlineVariant = Color(0xFF52443E)
private val DarkScrim = Color(0xFF000000)
private val DarkInverseSurface = Color(0xFFEBE1DE)
private val DarkInverseOnSurface = Color(0xFF201A19)
private val DarkInversePrimary = Color(0xFFA83D27)

// Light Theme Colors (Material 3)
private val LightPrimary = Color(0xFFA83D27)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFDCD1)
private val LightOnPrimaryContainer = Color(0xFF3E0D00)

private val LightSecondary = Color(0xFF78483F)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFFFDAC4)
private val LightOnSecondaryContainer = Color(0xFF2C1509)

private val LightTertiary = Color(0xFF9B5323)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFFFDCC7)
private val LightOnTertiaryContainer = Color(0xFF370C00)

private val LightError = Color(0xFFB3261E)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFF9DEDC)
private val LightOnErrorContainer = Color(0xFF410E0B)

private val LightBackground = Color(0xFFFCF9F6)
private val LightOnBackground = Color(0xFF201A19)
private val LightSurface = Color(0xFFFCF9F6)
private val LightOnSurface = Color(0xFF201A19)
private val LightSurfaceVariant = Color(0xFFF0DFD8)
private val LightOnSurfaceVariant = Color(0xFF52443E)
private val LightOutline = Color(0xFF85746B)
private val LightOutlineVariant = Color(0xFFD8C6BE)
private val LightScrim = Color(0xFF000000)
private val LightInverseSurface = Color(0xFF363030)
private val LightInverseOnSurface = Color(0xFFFCF9F6)
private val LightInversePrimary = Color(0xFFFFB5A3)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary
)

@Composable
fun BolSaafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BolSaafTypography,
        shapes = BolSaafShapes,
        content = content
    )
}
