package com.bolsaaf.audio

/**
 * PCM int16 ↔ float32 for DSP paths (RNNoise native already converts in JNI; use for tests / future Kotlin DSP).
 */
object PcmFormat {

    fun shortToFloat(input: ShortArray): FloatArray {
        return FloatArray(input.size) { i -> input[i] / 32768.0f }
    }

    fun floatToShort(input: FloatArray): ShortArray {
        return ShortArray(input.size) { i ->
            (input[i].coerceIn(-1f, 1f) * 32768.0f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
