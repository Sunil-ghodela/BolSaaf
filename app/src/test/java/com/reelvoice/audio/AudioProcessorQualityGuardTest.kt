package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-only tests for the dry-mix safety logic exposed via
 * [AudioProcessor.runQualityGuard]. Does not exercise native RNNoise —
 * synthesises an "over-suppressed" cleaned buffer directly and confirms
 * the guard blends the original back in.
 */
class AudioProcessorQualityGuardTest {

    @Test
    fun overSuppressedOutput_triggersDryMix() {
        val original = ShortArray(4800) { 8000 }
        val overSuppressed = ShortArray(4800) { 200 }

        val result = AudioProcessor.runQualityGuard(original, overSuppressed)

        assertTrue("expected dry-mix to kick in", result.appliedDryMix)
        // 0.7 * 200 + 0.3 * 8000 = 2540
        assertEquals(2540, result.finalBuffer[0].toInt())
    }

    @Test
    fun passingOutput_untouched() {
        val original = ShortArray(4800) { 5000 }
        val cleaned = ShortArray(4800) { 4500 }

        val result = AudioProcessor.runQualityGuard(original, cleaned)

        assertFalse("should not apply dry-mix on a passing guard", result.appliedDryMix)
        assertEquals(4500, result.finalBuffer[0].toInt())
        assertTrue(result.report.pass)
    }
}
