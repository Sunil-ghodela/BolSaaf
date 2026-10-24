package com.reelvoice.audio

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Test

class BoundedWavInputTest {
    private fun wav(size: Int, declaredSize: Int = size - 8): ByteArray = ByteArray(size).apply {
        "RIFF".toByteArray().copyInto(this)
        "WAVE".toByteArray().copyInto(this, 8)
        for (index in 0..3) this[4 + index] = (declaredSize ushr (8 * index)).toByte()
    }

    @Test fun nonWavReadsOnlyTwelveBytes() {
        var reads = 0
        val endlessNonWav = object : InputStream() {
            override fun read(): Int { reads++; return 0 }
        }
        assertNull(BoundedWavInput.read(endlessNonWav))
        assertEquals(12, reads)
    }

    @Test fun riffWithoutWaveIsNotWav() {
        val bytes = wav(40).apply { "AVI ".toByteArray().copyInto(this, 8) }
        val stream = ByteArrayInputStream(bytes)
        assertNull(BoundedWavInput.read(stream))
        assertEquals(28, stream.available())
    }

    @Test fun truncatedHeadersReturnNull() {
        val bytes = wav(12)
        for (size in 0..11) assertNull(BoundedWavInput.read(ByteArrayInputStream(bytes.copyOf(size))))
    }

    @Test fun validPayloadIsPreservedIncludingAtCap() {
        val bytes = wav(64).apply { for (i in 12 until size) this[i] = i.toByte() }
        assertArrayEquals(bytes, BoundedWavInput.read(ByteArrayInputStream(bytes), 64))
    }

    @Test fun oversizedDeclarationFailsBeforeReadingPayload() {
        val stream = ByteArrayInputStream(wav(100, Int.MAX_VALUE))
        assertThrows(IOException::class.java) { BoundedWavInput.read(stream, 64) }
        assertEquals(88, stream.available())
    }

    @Test fun understatedDeclarationCannotBypassCap() {
        val stream = ByteArrayInputStream(wav(100, 4))
        assertThrows(IOException::class.java) { BoundedWavInput.read(stream, 64) }
        assertEquals(35, stream.available()) // Only cap + one bytes consumed.
    }
}
