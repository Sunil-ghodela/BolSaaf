package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class FillerRemoverTest {

    private val sr = 48_000

    private fun silence(seconds: Float): ShortArray =
        ShortArray((seconds * sr).toInt())

    private fun tone(seconds: Float, amp: Double = 0.45): ShortArray {
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
        val r = FillerRemover.process(ShortArray(0), sr)
        assertEquals(0, r.output.size)
        assertEquals(0, r.fillersRemoved)
    }

    @Test
    fun all_speech_no_fillers_passes_through() {
        val input = tone(seconds = 2f)
        val r = FillerRemover.process(input, sr)
        assertEquals(0, r.fillersRemoved)
        assertEquals(input.size, r.output.size)
    }

    @Test
    fun short_island_flanked_by_silences_is_removed() {
        // Speech 0.8s · silence 0.3s · short filler 0.2s · silence 0.3s · speech 0.8s.
        val input = concat(
            tone(0.8f),
            silence(0.3f),
            tone(0.2f),     // <- should be trimmed (looks like an "um")
            silence(0.3f),
            tone(0.8f)
        )
        val r = FillerRemover.process(input, sr)
        assertEquals(1, r.fillersRemoved)
        assertTrue("output should be shorter than input", r.output.size < input.size)
        assertTrue("should remove at least 0.2 s", r.secondsRemoved > 0.18f)
    }

    @Test
    fun long_speech_island_is_not_trimmed() {
        // 0.8s island is well above the 350ms filler cap.
        val input = concat(tone(0.6f), silence(0.3f), tone(0.8f), silence(0.3f), tone(0.6f))
        val r = FillerRemover.process(input, sr)
        assertEquals(0, r.fillersRemoved)
    }

    @Test
    fun short_island_without_silence_on_one_side_is_kept() {
        // Missing silence between filler and right neighbour → shouldn't be trimmed.
        val input = concat(
            tone(0.8f),
            silence(0.3f),
            tone(0.2f),
            tone(0.8f)     // no silence before this
        )
        val r = FillerRemover.process(input, sr)
        assertEquals(0, r.fillersRemoved)
    }

    @Test
    fun leading_and_trailing_speech_islands_are_never_flagged() {
        // A short leading burst followed by silence + a long body. First island is
        // short but it's the head — must stay intact.
        val input = concat(
            tone(0.2f),     // head, short but protected
            silence(0.4f),
            tone(1.0f),
            silence(0.4f),
            tone(0.2f)      // tail, short but protected
        )
        val r = FillerRemover.process(input, sr)
        assertEquals(0, r.fillersRemoved)
        assertEquals(input.size, r.output.size)
    }

    @Test
    fun multiple_fillers_all_removed() {
        val input = concat(
            tone(0.6f),
            silence(0.3f), tone(0.2f), silence(0.3f),     // filler 1
            tone(0.8f),
            silence(0.3f), tone(0.18f), silence(0.3f),    // filler 2
            tone(0.6f)
        )
        val r = FillerRemover.process(input, sr)
        assertEquals(2, r.fillersRemoved)
        assertTrue(r.secondsRemoved > 0.3f)
    }
}
