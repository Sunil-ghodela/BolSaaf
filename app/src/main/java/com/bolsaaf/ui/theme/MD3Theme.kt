package com.bolsaaf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material Design 3 color tokens — wired to the brand red → purple → blue palette
// (see BolSaafPalette.kt for the source-of-truth swatches and BrandGradient helpers).

// Light Theme — matches the screenshot reference (warm off-white canvas, brand red primary).
private val LightPrimary = BrandRed
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFE2E5)
private val LightOnPrimaryContainer = Color(0xFF5A0F18)

private val LightSecondary = BrandPurple
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFF3E1FB)
private val LightOnSecondaryContainer = Color(0xFF3D0F4A)

private val LightTertiary = BrandBlue
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFDDE9FB)
private val LightOnTertiaryContainer = Color(0xFF0E2247)

private val LightError = Color(0xFFE63946)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFCE2E5)
private val LightOnErrorContainer = Color(0xFF5A0F18)

private val LightBackground = BackgroundDark           // #FAFAFA
private val LightOnBackground = TextPrimary            // #1A1A1F
private val LightSurface = BackgroundCard              // #FFFFFF
private val LightOnSurface = TextPrimary
private val LightSurfaceVariant = SurfaceStripe        // #F4F5F7
private val LightOnSurfaceVariant = TextSecondary      // #6B7280
private val LightOutline = Color(0xFFE5E7EB)
private val LightOutlineVariant = Color(0xFFEDEEF1)
private val LightScrim = Color(0xFF000000)
private val LightInverseSurface = Color(0xFF2A2B30)
private val LightInverseOnSurface = Color(0xFFFAFAFA)
private val LightInversePrimary = BrandRedLight

// Dark Theme — same brand stops, lifted for dark-surface contrast.
private val DarkPrimary = Color(0xFFFF8E97)
private val DarkOnPrimary = Color(0xFF5A0F18)
private val DarkPrimaryContainer = Color(0xFF7A1A28)
private val DarkOnPrimaryContainer = Color(0xFFFFE2E5)

private val DarkSecondary = Color(0xFFD9A0E8)
private val DarkOnSecondary = Color(0xFF3D0F4A)
private val DarkSecondaryContainer = Color(0xFF54235F)
private val DarkOnSecondaryContainer = Color(0xFFF3E1FB)

private val DarkTertiary = Color(0xFF9DC0F2)
private val DarkOnTertiary = Color(0xFF0E2247)
private val DarkTertiaryContainer = Color(0xFF1F3A6E)
private val DarkOnTertiaryContainer = Color(0xFFDDE9FB)

private val DarkError = Color(0xFFFF8E97)
private val DarkOnError = Color(0xFF5A0F18)
private val DarkErrorContainer = Color(0xFF7A1A28)
private val DarkOnErrorContainer = Color(0xFFFFE2E5)

private val DarkBackground = Color(0xFF121214)
private val DarkOnBackground = Color(0xFFEDEEF1)
private val DarkSurface = Color(0xFF1A1B1F)
private val DarkOnSurface = Color(0xFFEDEEF1)
private val DarkSurfaceVariant = Color(0xFF2A2B30)
private val DarkOnSurfaceVariant = Color(0xFFB8BCC4)
private val DarkOutline = Color(0xFF494A50)
private val DarkOutlineVariant = Color(0xFF2A2B30)
private val DarkScrim = Color(0xFF000000)
private val DarkInverseSurface = Color(0xFFEDEEF1)
private val DarkInverseOnSurface = Color(0xFF1A1B1F)
private val DarkInversePrimary = BrandRed

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
    // Brand currently ships light-only (matches screenshot reference).
    // Pass darkTheme = true explicitly to opt into the dark scheme.
    darkTheme: Boolean = false,
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
