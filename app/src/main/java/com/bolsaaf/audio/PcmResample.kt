package com.bolsaaf.audio

import kotlin.math.roundToInt

/**
 * Single place for **mic/file → 48 kHz mono** before RNNoise (48 kHz, 480 samples/frame).
 */
object PcmResample {
    const val TARGET_SAMPLE_RATE = 48_000
    const val FRAME_SAMPLES_48K = 480

    /**
     * Source samples needed (mono) so that [resampleMonoTo48k] yields at least [FRAME_SAMPLES_48K] samples
     * (10 ms at 48 kHz). Example: 44_100 Hz → 441 samples → 480 @ 48k.
     */
    fun requiredSourceSamples(srcRate: Int): Int {
        if (srcRate <= 0) return FRAME_SAMPLES_48K
        return (FRAME_SAMPLES_48K * srcRate + TARGET_SAMPLE_RATE - 1) / TARGET_SAMPLE_RATE
    }

    fun downmixToMono(interleaved: ShortArray, channels: Int): ShortArray {
        if (channels == 1) return interleaved
        val frames = interleaved.size / channels
        return ShortArray(frames) { fi ->
            var sum = 0L
            for (c in 0 until channels) {
                sum += interleaved[fi * channels + c].toInt()
            }
            (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun resampleMonoTo48k(mono: ShortArray, srcRate: Int): ShortArray {
        if (srcRate == TARGET_SAMPLE_RATE) return mono.copyOf(mono.size)
        if (srcRate <= 0) return mono.copyOf(mono.size)
        val srcLen = mono.size
        val dstLen = ((srcLen.toLong() * TARGET_SAMPLE_RATE + srcRate / 2) / srcRate).toInt().coerceAtLeast(1)
        return ShortArray(dstLen) { di ->
            val srcPos = (di.toDouble() * srcRate) / TARGET_SAMPLE_RATE
            val i0 = srcPos.toInt().coerceIn(0, srcLen - 1)
            val i1 = (i0 + 1).coerceAtMost(srcLen - 1)
            val frac = (srcPos - i0).toFloat()
            val f0 = mono[i0].toFloat()
            val f1 = mono[i1].toFloat()
            (f0 + (f1 - f0) * frac).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun resampleInterleavedTo48kMono(interleaved: ShortArray, srcRate: Int, channels: Int): ShortArray {
        val mono = downmixToMono(interleaved, channels)
        return resampleMonoTo48k(mono, srcRate)
    }
}
