package com.reelvoice.audio

/**
 * Cleaning mode — affects **input gain staging only** (before RNNoise). Core DSP chain stays fixed.
 */
enum class CleaningPreset(val label: String, val inputLinearScale: Float) {
    /** Balanced; targets ~−6 dBFS peak before denoise */
    NORMAL("Normal", 0.5f),
    /** More headroom (~−8 dB), calmer peaks — good for noisy environments */
    STRONG("Strong", 0.4f),
    /** Slightly hotter (~−5 dB) — more presence */
    STUDIO("Studio", 0.55f);

    /** Normal remains available to uploaded-file processing, not microphone recording. */
    fun forRecording(): CleaningPreset = if (this == NORMAL) STRONG else this

    companion object {
        val recordingPresets: List<CleaningPreset> = listOf(STRONG, STUDIO)

        fun fromIndex(i: Int): CleaningPreset = entries.getOrElse(i) { NORMAL }
    }
}
