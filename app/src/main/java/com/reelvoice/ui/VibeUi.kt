package com.reelvoice.ui

import com.reelvoice.audio.AdaptiveAudioAnalyzer
import java.util.Locale

/**
 * Display names + light heuristics for "Vibe" (background bed) UX.
 *
 * Server still keys on the same short ids ([cafe, rain, forest, ...]); we only
 * remap the *display label* to give the app an India-first voice. If the user
 * sees a server-side vibe id we don't know about, we fall back to the server
 * label to avoid breaking on new backend additions.
 */
object VibeUi {

    fun displayLabelForBackgroundId(id: String, serverLabel: String): String {
        val key = id.trim().lowercase(Locale.US)
        return when (key) {
            "ocean" -> "🌊 Ocean Calm"
            "cafe" -> "☕ Chai Shop"
            "rain" -> "🌧️ Monsoon Vibes"
            "forest" -> "🌿 Garden Morning"
            "street" -> "🛺 Delhi Street"
            "podcast" -> "🎙️ Podcast Room"
            "bhakti" -> "🕉️ Bhakti"
            "cricket" -> "🏏 Cricket Stadium"
            "confession" -> "🤫 Confession"
            "study" -> "📚 Study Lo-fi"
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
                profile.flags.contains("moderate_noise_near_zero") -> "Monsoon Vibes"
            profile.rmsDbfs < -42f -> "Ocean Calm"
            profile.flags.contains("loud_input") -> "Chai Shop"
            else -> "Garden Morning"
        }
        return "✨ Suggested: $name"
    }
}
