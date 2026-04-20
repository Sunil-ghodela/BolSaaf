package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class SilenceCutterTest {

    private val sr = 48_000

    private fun silence(seconds: Float): ShortArray =
        ShortArray((seconds * sr).toInt())

    private fun tone(seconds: Float, amp: Double = 0.6): ShortArray {
        val n = (seconds * sr).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val v = sin(2 * Math.PI * 440.0 * i / sr) * amp * Short.MAX_VALUE
            out[i] = v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun concat(vararg parts: ShortArray): ShortArray {
        val total = parts.sumOf { it.size }
        val out = ShortArray(total)
        var pos = 0
        for (p in parts) {
            System.arraycopy(p, 0, out, pos, p.size)
            pos += p.size
        }
        return out
    }

    @Test
    fun empty_input_returns_empty_report() {
        val r = SilenceCutter.process(ShortArray(0), sr)
        assertEquals(0, r.output.size)
        assertEquals(0, r.regionsTrimmed)
        assertEquals(0, r.samplesRemoved)
    }

    @Test
    fun all_tone_no_silence_is_untouched() {
        val input = tone(seconds = 2f)
        val r = SilenceCutter.process(input, sr)
        assertEquals(input.size, r.output.size)
        assertEquals(0, r.regionsTrimmed)
        assertEquals(0, r.samplesRemoved)
    }

    @Test
    fun short_pause_under_threshold_is_not_trimmed() {
        // 300 ms pause is below the default 500 ms threshold.
        val input = concat(tone(0.5f), silence(0.3f), tone(0.5f))
        val r = SilenceCutter.process(input, sr)
        assertEquals(0, r.regionsTrimmed)
        assertEquals(input.size, r.output.size)
    }

    @Test
    fun long_pause_is_trimmed_keeping_edge_padding() {
        // 2 s pause, default keepPauseSec = 0.18 s → keep 0.09 s on each side → remove ~1.82 s.
        val input = concat(tone(0.5f), silence(2f), tone(0.5f))
        val r = SilenceCutter.process(input, sr)
        assertEquals(1, r.regionsTrimmed)
        assertTrue("samples removed should be > 1.5 s worth, got ${r.samplesRemoved}",
            r.samplesRemoved > (1.5f * sr).toInt())
        assertTrue("seconds removed should be > 1.5 but was ${r.secondsRemoved}",
            r.secondsRemoved > 1.5f)
        // Total output should be original minus trim.
        assertEquals(input.size - r.samplesRemoved, r.output.size)
    }

    @Test
    fun multiple_long_pauses_all_get_trimmed() {
        val input = concat(
            tone(0.5f), silence(1f),      // pause 1
            tone(0.5f), silence(1.5f),    // pause 2
            tone(0.5f)
        )
        val r = SilenceCutter.process(input, sr)
        assertEquals(2, r.regionsTrimmed)
        assertTrue(r.secondsRemoved > 1.5f)
    }

    @Test
    fun only_silence_removed_is_middle_slice_not_tail_or_head() {
        // Leading tone should be preserved intact.
        val head = tone(0.4f)
        val pause = silence(2f)
        val tail = tone(0.4f)
        val input = concat(head, pause, tail)
        val r = SilenceCutter.process(input, sr)

        // Head first 0.3 s should match input first 0.3 s exactly (no mangling at the start).
        val probe = (0.3f * sr).toInt()
        for (i in 0 until probe) {
            assertEquals("sample $i diverges", input[i], r.output[i])
        }
        // Tail end 0.3 s of input should equal tail 0.3 s of output.
        for (i in 0 until probe) {
            val inIdx = input.size - 1 - i
            val outIdx = r.output.size - 1 - i
            assertEquals("tail sample $i diverges", input[inIdx], r.output[outIdx])
        }
    }
}
