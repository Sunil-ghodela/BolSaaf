package com.reelvoice.audio

import kotlin.math.abs
import kotlin.math.sign

/**
 * Phase 2: input attenuation + soft limiter **before** RNNoise to avoid 0 dBFS clipping and give headroom (~−6 dBFS typical).
 */
object AudioInputStage {

    fun apply(buffer: ShortArray, preset: CleaningPreset) {
        val g = preset.inputLinearScale
        for (i in buffer.indices) {
            var x = buffer[i] / 32768f * g
            x = softLimit(x)
            buffer[i] = (x * 32768f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun softLimit(x: Float): Float {
        val ax = abs(x)
        if (ax <= 0.88f) return x
        val s = sign(x)
        val over = ax - 0.88f
        return s * (0.88f + over / (1f + over * 5f))
    }
}
