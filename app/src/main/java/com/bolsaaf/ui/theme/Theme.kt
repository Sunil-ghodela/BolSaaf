package com.bolsaaf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ThemeBlue,
    onPrimary = Color.White,
    secondary = ThemeRed,
    onSecondary = Color.White,
    tertiary = ThemeBlueLight,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceStripe,
    onSurfaceVariant = TextSecondary,
    outline = SliderTrackStrong
)

@Composable
fun BolSaafTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
