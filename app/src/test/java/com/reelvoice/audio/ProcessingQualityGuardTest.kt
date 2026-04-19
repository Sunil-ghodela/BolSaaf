package com.reelvoice.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingQualityGuardTest {

    @Test
    fun heavyAttenuation_failsGuard() {
        val orig = ShortArray(4800) { 8000 }
        val cleaned = ShortArray(4800) { 200 }
        val r = ProcessingQualityGuard.compare(orig, cleaned)
        assertFalse(r.pass)
        assertTrue(r.issues.any { it == "heavy_rms_drop" })
    }

    @Test
    fun mildChange_passes() {
        val orig = ShortArray(4800) { 5000 }
        val cleaned = ShortArray(4800) { 4500 }
        val r = ProcessingQualityGuard.compare(orig, cleaned)
        assertTrue(r.pass)
    }
}
