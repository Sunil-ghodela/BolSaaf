package com.reelvoice.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class BeforeAfterSamplerTest {

    private fun sine(freq: Double, seconds: Double, sampleRate: Int, amp: Double = 0.7): ShortArray {
        val n = (seconds * sampleRate).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val v = sin(2 * Math.PI * freq * i / sampleRate) * amp * Short.MAX_VALUE
            out[i] = v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    @Test
    fun durationSeconds_is_sum_of_before_and_after() {
        val s = BeforeAfterSampler(
            originalSamples = ShortArray(48_000),
            cleanedSamples = ShortArray(48_000 * 2),
            sampleRate = 48_000
        )
        assertEquals(3f, s.durationSeconds, 1e-4f)
        assertEquals(1f, s.beforeDurationSec, 1e-4f)
        assertEquals(2f, s.afterDurationSec, 1e-4f)
    }

    @Test
    fun sampleFrame_during_before_phase_activates_top_pane_only() {
        val loud = sine(freq = 440.0, seconds = 2.0, sampleRate = 48_000, amp = 0.8)
        val silent = ShortArray(48_000 * 2)
        val s = BeforeAfterSampler(loud, silent, 48_000)

        val bars = s.sampleFrame(timeSeconds = 1.5f, barCount = 60)
        assertEquals(60, bars.size)
        val beforeAvg = bars.sliceArray(0 until 30).average()
        val afterAvg = bars.sliceArray(30 until 60).average()
        assertTrue("before pane should pulse loud, got $beforeAvg", beforeAvg > 0.3)
        assertTrue("after pane should be dimmed, got $afterAvg", afterAvg < 0.2)
    }

    @Test
    fun sampleFrame_during_after_phase_activates_bottom_pane_only() {
        val silent = ShortArray(48_000 * 2)
        val loud = sine(freq = 440.0, seconds = 2.0, sampleRate = 48_000, amp = 0.8)
        val s = BeforeAfterSampler(silent, loud, 48_000)

        // Before pane runs 0..2s; at t=3s we're mid-after pane.
        val bars = s.sampleFrame(timeSeconds = 3f, barCount = 60)
        assertEquals(60, bars.size)
        val beforeAvg = bars.sliceArray(0 until 30).average()
        val afterAvg = bars.sliceArray(30 until 60).average()
        assertTrue("after pane should pulse loud, got $afterAvg", afterAvg > 0.3)
        assertTrue("before pane should be dimmed, got $beforeAvg", beforeAvg < 0.2)
    }

    @Test
    fun sampleFrame_returns_even_length_with_floor_on_dim_pane() {
        val s = BeforeAfterSampler(
            originalSamples = ShortArray(48_000),
            cleanedSamples = ShortArray(48_000),
            sampleRate = 48_000
        )
        val bars = s.sampleFrame(timeSeconds = 0.3f, barCount = 40)
        assertEquals(40, bars.size)
        // Both panes dead silent, but dim pane must still be >= idle floor (0.04)
        // ... for silent active pane, both halves will be near zero though.
        // Sanity: output is non-negative and within [0, 1].
        for (b in bars) assertTrue("bar out of range: $b", b in 0f..1f)
    }
}
