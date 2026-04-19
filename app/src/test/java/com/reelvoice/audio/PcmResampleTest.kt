package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PcmResampleTest {

    @Test
    fun requiredSourceSamples_44100_yields441() {
        assertEquals(441, PcmResample.requiredSourceSamples(44_100))
    }

    @Test
    fun resampleMonoTo48k_441samples_from44100_to480() {
        val src = ShortArray(441) { 1000 }
        val out = PcmResample.resampleMonoTo48k(src, 44_100)
        assertEquals(PcmResample.FRAME_SAMPLES_48K, out.size)
    }

    @Test
    fun resampleMonoTo48k_480_at48k_unchangedLength() {
        val src = ShortArray(480) { (it * 10).toShort() }
        val out = PcmResample.resampleMonoTo48k(src, 48_000)
        assertEquals(480, out.size)
        assertEquals(src[0], out[0])
        assertEquals(src[479], out[479])
    }
}
