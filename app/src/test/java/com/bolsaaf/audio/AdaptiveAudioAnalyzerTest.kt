package com.bolsaaf.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class AdaptiveAudioAnalyzerTest {

    private fun sine48k(freq: Double, seconds: Double, amp: Float): ShortArray {
        val sr = 48000
        val n = (sr * seconds).roundToInt()
        return ShortArray(n) { i ->
            (amp * 32767f * sin(2 * PI * freq * i / sr)).roundToInt().coerceIn(-32768, 32767).toShort()
        }
    }

    @Test
    fun veryQuietSignal_choosesStudioPipeline() {
        val s = sine48k(440.0, 0.5, 0.00025f)
        val p = AdaptiveAudioAnalyzer.analyze(s, 48000)
        assertEquals("studio", p.suggestedCloudMode)
        assertEquals(CleaningPreset.STUDIO, p.suggestedCleaningPreset)
        assertTrue(p.flags.any { it == "very_low_rms" })
        assertTrue(p.adaptivePreset.preGain >= 3.0)
    }

    @Test
    fun loudSignal_reducesPregainAndUsesStandardMode() {
        val s = sine48k(440.0, 0.5, 0.7f)
        val p = AdaptiveAudioAnalyzer.analyze(s, 48000)
        assertEquals("standard", p.suggestedCloudMode)
        assertTrue(p.flags.any { it == "loud_input" })
        assertTrue(p.adaptivePreset.preGain <= 1.5)
    }

    @Test
    fun clippedSignal_setsStrongCompressor() {
        val s = ShortArray(48000) { 32000 }
        val p = AdaptiveAudioAnalyzer.analyze(s, 48000)
        assertTrue(p.flags.any { it == "clipping_risk" })
        assertEquals("STRONG", p.adaptivePreset.compressorStrength)
    }

    @Test
    fun pcm16Bytes_roundTrip() {
        val s = shortArrayOf(0, -1000, 1000, -32768, 32767)
        val bb = java.nio.ByteBuffer.allocate(s.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (v in s) bb.putShort(v)
        val back = bb.array().pcm16LeToShortArray()
        assertEquals(s.size, back.size)
        for (i in s.indices) assertEquals(s[i], back[i])
    }
}
