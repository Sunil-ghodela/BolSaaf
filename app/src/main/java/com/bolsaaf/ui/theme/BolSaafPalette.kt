package com.bolsaaf.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Main app canvas — pale yellow (matches design refs ~#FFFBEB). */
val BackgroundDark = Color(0xFFFFFBEB)

/** Cards / sheets — clean white on yellow. */
val BackgroundCard = Color(0xFFFFFFFF)

val SurfaceStripe = Color(0xFFFFF7ED)

/** Brand red → blue (filters, logo, progress, selected chips). */
val ThemeRed = Color(0xFFE34C52)
val ThemeBlue = Color(0xFF537FE7)
val ThemeRedLight = Color(0xFFFF6B5C)
val ThemeBlueLight = Color(0xFF6B99FF)
val TitleVideoAccent = Color(0xFFE64A19)

val PrimaryGradient = Brush.linearGradient(
    colors = listOf(ThemeRed, ThemeBlue),
    start = Offset(0f, 0f),
    end = Offset(420f, 280f)
)

/** Primary “Clean / action” CTA — orange → red (mock home button). */
val CtaOrangeRedGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF8C42), Color(0xFFFF4B2B))
)

val SubtitleBluePurple = Brush.horizontalGradient(
    colors = listOf(ThemeBlue, Color(0xFF7E57C2))
)

/** Soft red–blue blend for smaller chips (keeps old names for call sites). */
val AccentPurple = ThemeRedLight
val AccentGreen = ThemeBlue
val AccentCyan = ThemeBlueLight

val TextPrimary = Color(0xFF263238)
val TextSecondary = Color(0xFF546E7A)

val NavUnselected = Color(0xFF78909C)

/** Dialogs / overlays on warm UI */
val PanelOverlay = Color(0xFFFFFDE7).copy(alpha = 0.98f)
val DialogScrim = Color(0xFF37474F).copy(alpha = 0.42f)

val SliderTrack = Color(0xFFFFE0B2)
val SliderTrackStrong = Color(0xFFFFCC80)
