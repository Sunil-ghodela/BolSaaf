package com.reelvoice.audio

/**
 * Post-clean "voice style" — shapes the cleaned voice with a small biquad
 * chain. Purely additive to [CleaningPreset] (which only gates input gain
 * into RNNoise). Applied on-device, no server round-trip.
 */
enum class VoiceStyle(
    val id: String,
    val label: String,
    val emoji: String,
    val tagline: String,
) {
    NONE("none", "Original", "✨", "No extra colouring"),
    PODCAST("podcast", "Podcast", "🎙️", "Warm, broadcast-ready"),
    CINEMATIC("cinematic", "Cinematic", "🎬", "Deep, dramatic presence"),
    CALM("calm", "Calm", "🧘", "Soft, gentle on the ears");

    companion object {
        fun fromId(id: String?): VoiceStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
    }
}

/**
 * Applies the biquad chain for the selected [VoiceStyle] to mono 16-bit PCM.
 *
 * Each style's chain is deliberately mild (2–4 biquads max) so it sounds
 * *subtle* rather than "effects-heavy" — the cleaned voice is already the
 * star; style just tints it.
 */
object VoiceStyleProcessor {

    fun process(input: ShortArray, sampleRate: Int, style: VoiceStyle): ShortArray {
        if (style == VoiceStyle.NONE || input.isEmpty() || sampleRate <= 0) {
            return input
        }
        val filters = chainFor(style, sampleRate)
        if (filters.isEmpty()) return input

        val out = ShortArray(input.size)
        for (i in input.indices) {
            var s = input[i].toDouble()
            for (f in filters) s = f.process(s)
            out[i] = s.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun chainFor(style: VoiceStyle, sr: Int): List<BiquadFilter> {
        val sample = sr
        return when (style) {
            VoiceStyle.NONE -> emptyList()
            VoiceStyle.PODCAST -> listOf(
                BiquadFilter(Biquad.highPass(sample, 80.0)),
                BiquadFilter(Biquad.lowShelf(sample, 200.0, gainDb = 2.0)),
                BiquadFilter(Biquad.peakingEq(sample, 3500.0, gainDb = -1.5, q = 1.2)),
                BiquadFilter(Biquad.highShelf(sample, 8000.0, gainDb = -2.0)),
            )
            VoiceStyle.CINEMATIC -> listOf(
                BiquadFilter(Biquad.highPass(sample, 55.0)),
                BiquadFilter(Biquad.lowShelf(sample, 120.0, gainDb = 3.0)),
                BiquadFilter(Biquad.peakingEq(sample, 800.0, gainDb = -1.0, q = 1.0)),
            )
            VoiceStyle.CALM -> listOf(
                BiquadFilter(Biquad.highShelf(sample, 6000.0, gainDb = -2.0)),
                BiquadFilter(Biquad.peakingEq(sample, 3000.0, gainDb = -1.5, q = 1.2)),
            )
        }
    }
}
