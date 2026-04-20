package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class VoiceStyleTest {

    private val sr = 48_000

    private fun tone(freq: Double, seconds: Double, amp: Double = 0.4): ShortArray {
        val n = (seconds * sr).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val v = sin(2 * Math.PI * freq * i / sr) * amp * Short.MAX_VALUE
            out[i] = v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun rms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var acc = 0.0
        for (s in samples) {
            val v = s / 32768.0
            acc += v * v
        }
        return kotlin.math.sqrt(acc / samples.size)
    }

    @Test
    fun style_NONE_is_a_passthrough() {
        val input = tone(freq = 440.0, seconds = 0.5)
        val out = VoiceStyleProcessor.process(input, sr, VoiceStyle.NONE)
        assertTrue(input.contentEquals(out))
    }

    @Test
    fun podcast_style_attenuates_sub_bass_rumble() {
        // 40 Hz rumble should be cut substantially (high-pass at 80 Hz).
        val rumble = tone(freq = 40.0, seconds = 1.0, amp = 0.5)
        val styled = VoiceStyleProcessor.process(rumble, sr, VoiceStyle.PODCAST)
        val dropRatio = rms(styled) / rms(rumble)
        assertTrue("podcast should attenuate 40Hz substantially, got ratio=$dropRatio", dropRatio < 0.5)
    }

    @Test
    fun cinematic_style_boosts_low_end_body() {
        // 120 Hz tone should gain energy with the +3 dB low-shelf.
        val body = tone(freq = 120.0, seconds = 1.0, amp = 0.3)
        val styled = VoiceStyleProcessor.process(body, sr, VoiceStyle.CINEMATIC)
        val gainRatio = rms(styled) / rms(body)
        assertTrue("cinematic should boost 120Hz, got ratio=$gainRatio", gainRatio > 1.05)
    }

    @Test
    fun calm_style_softens_high_end_sibilance() {
        // 8 kHz tone should be attenuated by the high-shelf cut.
        val sibilance = tone(freq = 8000.0, seconds = 0.5, amp = 0.4)
        val styled = VoiceStyleProcessor.process(sibilance, sr, VoiceStyle.CALM)
        val ratio = rms(styled) / rms(sibilance)
        assertTrue("calm should attenuate 8kHz, got ratio=$ratio", ratio < 0.95)
    }

    @Test
    fun styling_returns_same_size_array() {
        val input = tone(freq = 1000.0, seconds = 0.25)
        for (style in VoiceStyle.entries) {
            val out = VoiceStyleProcessor.process(input, sr, style)
            assertEquals("size mismatch for $style", input.size, out.size)
        }
    }

    @Test
    fun empty_input_passes_through_for_any_style() {
        for (style in VoiceStyle.entries) {
            val out = VoiceStyleProcessor.process(ShortArray(0), sr, style)
            assertEquals(0, out.size)
        }
    }

    @Test
    fun fromId_resolves_known_ids_and_defaults_to_none() {
        assertEquals(VoiceStyle.PODCAST, VoiceStyle.fromId("podcast"))
        assertEquals(VoiceStyle.CINEMATIC, VoiceStyle.fromId("Cinematic"))
        assertEquals(VoiceStyle.CALM, VoiceStyle.fromId("CALM"))
        assertEquals(VoiceStyle.NONE, VoiceStyle.fromId("nonsense"))
        assertEquals(VoiceStyle.NONE, VoiceStyle.fromId(null))
    }

    @Test
    fun podcast_and_calm_produce_different_outputs() {
        val mid = tone(freq = 1000.0, seconds = 0.25, amp = 0.4)
        val podcastOut = VoiceStyleProcessor.process(mid, sr, VoiceStyle.PODCAST)
        val calmOut = VoiceStyleProcessor.process(mid, sr, VoiceStyle.CALM)
        assertFalse(
            "podcast and calm should differ on a tone they both process",
            podcastOut.contentEquals(calmOut)
        )
        assertNotEquals(rms(podcastOut), rms(calmOut), 1e-6)
    }
}
