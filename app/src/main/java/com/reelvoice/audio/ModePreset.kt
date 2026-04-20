package com.reelvoice.audio

/**
 * One-tap "vibe bundle" — picks a background id + cleaning preset + BG mix level
 * in one gesture, so creators don't have to hand-tune each knob.
 *
 * Mode presets are deliberately lightweight: no custom DSP, just smart defaults
 * that map to existing server backgrounds + client-side cleaning options.
 * Adding a new mode (Cricket / Confession / Study Vlog / …) is just appending to
 * [ModePresets.ALL] once the matching server-side BG audio ships.
 */
data class ModePreset(
    val id: String,
    val displayLabel: String,
    val subtitle: String,
    val emoji: String,
    /** Server BG id we want to mix in. Falls back to closest match if missing. */
    val preferredBackgroundId: String,
    /** Backgrounds accepted if [preferredBackgroundId] isn't returned by the server. */
    val fallbackBackgroundIds: List<String>,
    /** Cleaning preset applied to the voice. */
    val cleaningPreset: CleaningPreset,
    /** Recommended BG mix volume (0..1, server clamps to [0.02..0.5]). */
    val bgMixDefault: Float,
)

object ModePresets {

    val BHAKTI = ModePreset(
        id = "bhakti",
        displayLabel = "Bhakti",
        subtitle = "Temple bells · calm voice",
        emoji = "🕉️",
        preferredBackgroundId = "bhakti",
        fallbackBackgroundIds = listOf("ocean", "forest", "rain"),
        cleaningPreset = CleaningPreset.STRONG,
        bgMixDefault = 0.25f,
    )

    val CRICKET = ModePreset(
        id = "cricket",
        displayLabel = "Cricket",
        subtitle = "Stadium roar · bold voice",
        emoji = "🏏",
        preferredBackgroundId = "cricket",
        fallbackBackgroundIds = listOf("street", "cafe"),
        cleaningPreset = CleaningPreset.STUDIO,
        bgMixDefault = 0.30f,
    )

    val CONFESSION = ModePreset(
        id = "confession",
        displayLabel = "Confession",
        subtitle = "Whispered · just for you",
        emoji = "🤫",
        preferredBackgroundId = "confession",
        fallbackBackgroundIds = listOf("ocean", "rain"),
        cleaningPreset = CleaningPreset.STRONG,
        bgMixDefault = 0.12f,
    )

    val STUDY_VLOG = ModePreset(
        id = "study",
        displayLabel = "Study",
        subtitle = "Lo-fi focus · exam grind",
        emoji = "📚",
        preferredBackgroundId = "study",
        fallbackBackgroundIds = listOf("rain", "cafe", "forest"),
        cleaningPreset = CleaningPreset.NORMAL,
        bgMixDefault = 0.22f,
    )

    val ALL: List<ModePreset> = listOf(BHAKTI, CRICKET, CONFESSION, STUDY_VLOG)
}
