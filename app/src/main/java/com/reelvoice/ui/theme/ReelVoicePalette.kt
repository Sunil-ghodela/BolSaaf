package com.reelvoice.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand canvas — soft warm off-white (matches the launch screenshot).
val BackgroundDark = Color(0xFFFAFAFA)

// Cards / sheets — clean white on the off-white canvas.
val BackgroundCard = Color(0xFFFFFFFF)

val SurfaceStripe = Color(0xFFF4F5F7)

// Brand stops — red → purple → blue (the display-headline gradient).
val BrandRed = Color(0xFFE94E5B)
val BrandPurple = Color(0xFFA24CB7)
val BrandBlue = Color(0xFF3D7DDB)

// Light variants used in soft tints / chip backgrounds.
val BrandRedLight = Color(0xFFFF7B85)
val BrandBlueLight = Color(0xFF6FA3F0)
val BrandPurpleLight = Color(0xFFC78BD8)

// Make Reel CTA stops — warm orange → red.
val MakeReelOrange = Color(0xFFFF6B35)
val MakeReelRed = Color(0xFFFF3D5B)

// Back-compat aliases — older call sites already import these names.
val ThemeRed = BrandRed
val ThemeBlue = BrandBlue
val ThemeRedLight = BrandRedLight
val ThemeBlueLight = BrandBlueLight
val TitleVideoAccent = MakeReelOrange

// Linear (top-left → bottom-right) brand wash for logo / progress / fills.
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(BrandRed, BrandPurple, BrandBlue),
    start = Offset(0f, 0f),
    end = Offset(420f, 280f)
)

// Primary "Make Reel / hero CTA" — orange → red (matches screenshot).
val CtaOrangeRedGradient = Brush.horizontalGradient(
    colors = listOf(MakeReelOrange, MakeReelRed)
)

// Subtitle / display text gradient — same brand stops, horizontal.
val SubtitleBluePurple = Brush.horizontalGradient(
    colors = listOf(BrandRed, BrandPurple, BrandBlue)
)

// Old chip names — repointed to brand stops so call sites still work.
val AccentPurple = BrandPurple
val AccentGreen = BrandBlue
val AccentCyan = BrandBlueLight

val TextPrimary = Color(0xFF1A1A1F)
val TextSecondary = Color(0xFF6B7280)

val NavUnselected = Color(0xFF9AA0A6)

val PanelOverlay = Color(0xFFFFFFFF).copy(alpha = 0.98f)
val DialogScrim = Color(0xFF000000).copy(alpha = 0.42f)

val SliderTrack = Color(0xFFE5E7EB)
val SliderTrackStrong = Color(0xFFD1D5DB)

/**
 * Centralized brand gradients. Prefer these over Brush.horizontalGradient(listOf(Color(...))).
 *
 * - [Brand]      — red → purple → blue. Use for selected pills, sliders, avatar rings,
 *                  display-text accents (the title in the screenshot).
 * - [MakeReel]   — orange → red. Reserve for the hero "Make Reel" / primary CTA only.
 * - [BrandSoft]  — same brand stops at low alpha for backgrounds / haloes / card strips.
 */
object BrandGradient {
    val Brand: Brush = Brush.horizontalGradient(
        colors = listOf(BrandRed, BrandPurple, BrandBlue)
    )

    val BrandLinear: Brush = Brush.linearGradient(
        colors = listOf(BrandRed, BrandPurple, BrandBlue),
        start = Offset(0f, 0f),
        end = Offset(420f, 280f)
    )

    val MakeReel: Brush = Brush.horizontalGradient(
        colors = listOf(MakeReelOrange, MakeReelRed)
    )

    val BrandSoft: Brush = Brush.horizontalGradient(
        colors = listOf(
            BrandRed.copy(alpha = 0.12f),
            BrandPurple.copy(alpha = 0.10f),
            BrandBlue.copy(alpha = 0.12f)
        )
    )
}
