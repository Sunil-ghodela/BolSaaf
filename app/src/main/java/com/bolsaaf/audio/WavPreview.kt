package com.bolsaaf.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Lightweight RMS envelope bars for waveform preview (16-bit mono WAV).
 */
object WavPreview {

    fun loadBars(file: File, barCount: Int = 40): List<Float> {
        if (!file.exists() || file.length() < 44) return defaultBars(barCount)
        return try {
            val bytes = file.readBytes()
            if (bytes[0] != 'R'.code.toByte()) return defaultBars(barCount)
            var offset = 12
            var dataStart = -1
            var dataSize = 0
            var numChannels = 1
            chunkLoop@ while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val dataOffset = offset + 8
                when (chunkId) {
                    "fmt " -> {
                        if (dataOffset + 16 <= bytes.size) {
                            val fmt = ByteBuffer.wrap(bytes, dataOffset, chunkSize.coerceAtMost(32))
                                .order(ByteOrder.LITTLE_ENDIAN)
                            val af = fmt.short.toInt() and 0xffff
                            if (af != 1) return defaultBars(barCount)
                            numChannels = fmt.short.toInt() and 0xffff
                        }
                    }
                    "data" -> {
                        dataStart = dataOffset
                        dataSize = chunkSize
                        break@chunkLoop
                    }
                }
                offset = dataOffset + chunkSize + (chunkSize and 1)
            }
            if (dataStart < 0 || numChannels < 1) return defaultBars(barCount)
            val nShorts = dataSize / 2
            if (nShorts <= 0) return defaultBars(barCount)
            val frames = nShorts / numChannels
            val samplesPerBar = (frames / barCount).coerceAtLeast(1)
            val out = FloatArray(barCount)
            var bi = 0
            var frameIdx = 0
            while (bi < barCount && frameIdx < frames) {
                var acc = 0.0
                var cnt = 0
                val end = (frameIdx + samplesPerBar).coerceAtMost(frames)
                var f = frameIdx
                while (f < end) {
                    val p = dataStart + f * numChannels * 2
                    if (p + 1 >= bytes.size) break
                    val v = (bytes[p].toInt() and 0xff) or ((bytes[p + 1].toInt() and 0xff) shl 8)
                    val s = (v shl 16) shr 16
                    val norm = s / 32768.0
                    acc += norm * norm
                    cnt++
                    f++
                }
                val rms = if (cnt > 0) sqrt(acc / cnt) else 0.0
                out[bi] = rms.toFloat().coerceIn(0.02f, 1f)
                bi++
                frameIdx = end
            }
            while (bi < barCount) {
                out[bi] = out[bi - 1].coerceAtLeast(0.12f)
                bi++
            }
            val max = out.maxOrNull() ?: 1f
            out.map { (it / max).coerceIn(0.12f, 1f) }
        } catch (_: Exception) {
            defaultBars(barCount)
        }
    }

    private fun defaultBars(n: Int) = List(n) { 0.15f }
}
