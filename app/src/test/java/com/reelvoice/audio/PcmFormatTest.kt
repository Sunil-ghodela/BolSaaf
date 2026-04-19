package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PcmFormatTest {

    @Test
    fun shortFloatRoundTrip_smallValues() {
        val s = shortArrayOf(0, 1000, (-1000).toShort())
        val back = PcmFormat.floatToShort(PcmFormat.shortToFloat(s))
        assertEquals(s[0], back[0])
        assertEquals(s[1], back[1])
        assertEquals(s[2], back[2])
    }
}
