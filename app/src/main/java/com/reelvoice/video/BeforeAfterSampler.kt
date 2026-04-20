package com.reelvoice.video

import kotlin.math.max

/**
 * Two-pane sampler for the "Before / After" video export. Concatenates the
 * original + cleaned PCM so [WaveformVideoEncoder] can write them as a
 * single audio track (original plays first, cleaned plays second), and
 * overrides [sampleFrame] to return a doubled-up amplitude array where the
 * first half represents the top (Before) pane and the second half represents
 * the bottom (After) pane.
 *
 * Only the pane whose audio is currently playing is "alive" — its bars pulse
 * to the real RMS envelope. The other pane is shown dimmed (amplitude scaled
 * to [INACTIVE_FACTOR]) so the viewer still sees it, but the active pane is
 * visually the focus.
 */
class BeforeAfterSampler(
    originalSamples: ShortArray,
    cleanedSamples: ShortArray,
    sampleRate: Int
) : WaveformWindowSampler(
    samples = originalSamples + cleanedSamples,
    sampleRate = sampleRate
) {

    private val originalSampler = WaveformWindowSampler(originalSamples, sampleRate)
    private val cleanedSampler = WaveformWindowSampler(cleanedSamples, sampleRate)
    val beforeDurationSec: Float = originalSampler.durationSeconds
    val afterDurationSec: Float = cleanedSampler.durationSeconds

    override fun sampleFrame(
        timeSeconds: Float,
        barCount: Int,
        windowSpanSeconds: Float,
        shape: Float
    ): FloatArray {
        val perPane = max(1, barCount / 2)
        val out = FloatArray(perPane * 2)
        val beforeActive = timeSeconds < beforeDurationSec

        if (beforeActive) {
            val active = originalSampler.sampleFrame(timeSeconds, perPane, windowSpanSeconds, shape)
            fillPane(out, 0, active, isActive = true)
            fillPane(out, perPane, active, isActive = false)
        } else {
            val localT = (timeSeconds - beforeDurationSec).coerceAtLeast(0f)
            val active = cleanedSampler.sampleFrame(localT, perPane, windowSpanSeconds, shape)
            fillPane(out, perPane, active, isActive = true)
            fillPane(out, 0, active, isActive = false)
        }
        return out
    }

    private fun fillPane(
        dst: FloatArray,
        offset: Int,
        active: FloatArray,
        isActive: Boolean
    ) {
        if (isActive) {
            for (i in active.indices) dst[offset + i] = active[i]
        } else {
            for (i in active.indices) {
                dst[offset + i] = (active[i] * INACTIVE_FACTOR).coerceAtLeast(IDLE_FLOOR)
            }
        }
    }

    companion object {
        /** Fraction of the active amplitude shown on the opposite (dim) pane. */
        private const val INACTIVE_FACTOR = 0.16f
        /** Minimum bar height on the dim pane so it never collapses to zero. */
        private const val IDLE_FLOOR = 0.04f
    }
}
