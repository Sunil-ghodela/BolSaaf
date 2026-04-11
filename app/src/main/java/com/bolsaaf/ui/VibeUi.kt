package com.bolsaaf.ui

import com.bolsaaf.audio.AdaptiveAudioAnalyzer
import java.util.Locale

/** Display names + light heuristics for "Vibe" (background bed) UX. Server still uses bg ids. */
object VibeUi {

    fun displayLabelForBackgroundId(id: String, serverLabel: String): String {
        val key = id.trim().lowercase(Locale.US)
        return when (key) {
            "ocean" -> "🌊 Ganga Calm"
            "cafe" -> "☕ Cafe Talk"
            "rain" -> "🌧️ Rain Focus"
            "forest" -> "🌲 Forest Peace"
            "street" -> "🛣️ Street buzz"
            "podcast" -> "🎙️ Podcast room"
            else -> serverLabel.ifBlank { id }
        }
    }

    /**
     * One-line hint from adaptive analysis (noise / level). Names match [displayLabelForBackgroundId] vibes.
     */
    fun suggestedVibeLine(profile: AdaptiveAudioAnalyzer.Profile?): String? {
        if (profile == null) return null
        if (profile.flags.contains("empty_signal")) return null
        val name = when {
            profile.flags.contains("high_noise_near_zero") ||
                profile.flags.contains("moderate_noise_near_zero") -> "Rain Focus"
            profile.rmsDbfs < -42f -> "Ganga Calm"
            profile.flags.contains("loud_input") -> "Cafe Talk"
            else -> "Forest Peace"
        }
        return "✨ Suggested: $name"
    }
}
