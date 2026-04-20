package com.reelvoice.video

/**
 * Visual recipe for a waveform video. Bundles title + subtitle text overrides
 * plus a full palette (bg gradient, bar gradient, text + watermark colors) so
 * the user can one-tap a vibe instead of living with the default brand look.
 *
 * Consumed by [WaveformFrameRenderer] via its optional template parameter.
 */
data class ReelTemplate(
    val id: String,
    val displayLabel: String,
    val tagline: String,
    val emoji: String,
    val titleText: String,
    val subtitleText: String,
    /** 3 stops for the linear background gradient (top-left → bottom-right). */
    val bgStops: IntArray,
    /** 3 stops for the vertical bar gradient (top → bottom). */
    val barStops: IntArray,
    val textColor: Int,
    val watermarkColor: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReelTemplate) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

object ReelTemplates {

    val VOICE_NOTE = ReelTemplate(
        id = "voice_note",
        displayLabel = "Voice Note",
        tagline = "Classic brand · minimal",
        emoji = "🎙️",
        titleText = "ReelVoice",
        subtitleText = "cleaned with AI",
        bgStops = intArrayOf(0xFFFDEBEE.toInt(), 0xFFF4E8F7.toInt(), 0xFFE8F1FB.toInt()),
        barStops = intArrayOf(0xFFE94E5B.toInt(), 0xFFA24CB7.toInt(), 0xFF3D7DDB.toInt()),
        textColor = 0xFF111111.toInt(),
        watermarkColor = 0x66000000,
    )

    val MOTIVATION = ReelTemplate(
        id = "motivation",
        displayLabel = "Motivation",
        tagline = "Warm gold · bold",
        emoji = "💪",
        titleText = "Your voice matters",
        subtitleText = "ReelVoice",
        bgStops = intArrayOf(0xFFFFF5E1.toInt(), 0xFFFFE1B4.toInt(), 0xFFFFD180.toInt()),
        barStops = intArrayOf(0xFFFF8A00.toInt(), 0xFFFF6A00.toInt(), 0xFFB44500.toInt()),
        textColor = 0xFF3A2200.toInt(),
        watermarkColor = 0x994A2A00.toInt(),
    )

    val STORY = ReelTemplate(
        id = "story",
        displayLabel = "Story",
        tagline = "Pastel purple · gentle",
        emoji = "📖",
        titleText = "My story",
        subtitleText = "in my own voice",
        bgStops = intArrayOf(0xFFF3E8FF.toInt(), 0xFFE9DBFF.toInt(), 0xFFDDD4FF.toInt()),
        barStops = intArrayOf(0xFF9B5CFF.toInt(), 0xFF7C3AED.toInt(), 0xFF5B21B6.toInt()),
        textColor = 0xFF2D1458.toInt(),
        watermarkColor = 0x993A1A7A.toInt(),
    )

    val ALL: List<ReelTemplate> = listOf(VOICE_NOTE, MOTIVATION, STORY)
}
