package com.reelvoice.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class WaveformWindowSamplerTest {

    private fun sine(freq: Double, seconds: Double, sampleRate: Int, amp: Double = 0.5): ShortArray {
        val n = (seconds * sampleRate).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val v = sin(2 * Math.PI * freq * i / sampleRate) * amp * Short.MAX_VALUE
            out[i] = v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    @Test
    fun duration_matches_sampleCount_over_sampleRate() {
        val samples = ShortArray(48_000)
        val s = WaveformWindowSampler(samples, 48_000)
        assertEquals(1.0f, s.durationSeconds, 1e-4f)
    }

    @Test
    fun sampleFrame_silence_returnsZeroBars() {
        val samples = ShortArray(48_000)
        val s = WaveformWindowSampler(samples, 48_000)
        val bars = s.sampleFrame(timeSeconds = 0.5f, barCount = 32)
        assertEquals(32, bars.size)
        for (b in bars) assertEquals(0f, b, 1e-5f)
    }

    @Test
    fun sampleFrame_loudSine_producesHighAmplitudeBars() {
        val samples = sine(freq = 440.0, seconds = 2.0, sampleRate = 48_000, amp = 0.8)
        val s = WaveformWindowSampler(samples, 48_000)
        val bars = s.sampleFrame(timeSeconds = 1.0f, barCount = 32)
        // All bars should be well above zero; the log compression should pull them toward ~0.6+
        val avg = bars.average().toFloat()
        assertTrue("expected average bar >0.3 for loud sine but got $avg", avg > 0.3f)
    }

    @Test
    fun sampleFrame_beforeAudioStart_bucketsClampedToZero() {
        val samples = sine(440.0, 1.0, 48_000, 0.8)
        val s = WaveformWindowSampler(samples, 48_000)
        // time=0 with default 1.2s window → entire window is before the start
        val bars = s.sampleFrame(timeSeconds = 0f, barCount = 16)
        for (b in bars) assertEquals(0f, b, 1e-5f)
    }

    @Test
    fun sampleAllFrames_framesMatchFpsTimesDuration() {
        val samples = ShortArray(48_000)
        val s = WaveformWindowSampler(samples, 48_000)
        val frames = s.sampleAllFrames(fps = 30, totalFrames = 30, barCount = 8)
        assertEquals(30, frames.size)
        assertEquals(8, frames[0].size)
    }

    @Test
    fun sampleFrame_zeroBarCount_throws() {
        val s = WaveformWindowSampler(ShortArray(1000), 48_000)
        try {
            s.sampleFrame(0.5f, 0)
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // ok
        }
    }

    @Test
    fun sampleFrame_emptySamples_returnsZeros() {
        val s = WaveformWindowSampler(ShortArray(0), 48_000)
        val bars = s.sampleFrame(0f, 16)
        assertEquals(16, bars.size)
        for (b in bars) assertEquals(0f, b, 1e-5f)
    }
}
