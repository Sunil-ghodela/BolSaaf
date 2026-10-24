package com.reelvoice.audio

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Bounds the direct WAV path before allocating PCM buffers. Non-WAV imports use MediaCodec. */
internal object BoundedWavInput {
    // Leaves room for PCM conversion and processing buffers on memory-constrained devices.
    const val MAX_BYTES = 16 * 1024 * 1024

    fun read(input: InputStream, maxBytes: Int = MAX_BYTES): ByteArray? {
        require(maxBytes >= 12)
        val header = ByteArray(12)
        var headerSize = 0
        while (headerSize < header.size) {
            val value = input.read()
            if (value == -1) return null
            header[headerSize++] = value.toByte()
        }
        if (String(header, 0, 4, Charsets.US_ASCII) != "RIFF" ||
            String(header, 8, 4, Charsets.US_ASCII) != "WAVE"
        ) return null

        val declaredSize = (4..7).fold(0L) { size, index ->
            size or ((header[index].toLong() and 0xff) shl ((index - 4) * 8))
        } + 8L
        if (declaredSize > maxBytes) throw IOException("WAV exceeds the 16 MiB import limit")

        val output = ByteArrayOutputStream(minOf(8192, maxBytes))
        output.write(header)
        val buffer = ByteArray(8192)
        while (true) {
            // Probe one byte beyond the cap, never buffer an oversized stream.
            val count = input.read(buffer, 0, minOf(buffer.size, maxBytes - output.size() + 1))
            if (count == -1) break
            if (count == 0) {
                val value = input.read()
                if (value == -1) break
                if (output.size() == maxBytes) throw IOException("WAV exceeds the 16 MiB import limit")
                output.write(value)
            } else {
                if (count > maxBytes - output.size()) throw IOException("WAV exceeds the 16 MiB import limit")
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }
}
