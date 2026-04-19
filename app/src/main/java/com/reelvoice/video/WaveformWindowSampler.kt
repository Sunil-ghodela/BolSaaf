package com.reelvoice.video

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Pure Kotlin sampler: turns a decoded 16-bit PCM buffer into per-frame bar-amplitude
 * arrays for the waveform video renderer.
 *
 * The sampler is presentation-agnostic — the renderer decides how to paint the bars.
 */
class WaveformWindowSampler(
    val samples: ShortArray,
    val sampleRate: Int
) {

    val durationSeconds: Float = if (sampleRate > 0) samples.size.toFloat() / sampleRate else 0f

    /**
     * Bars for a single frame at [timeSeconds]. Returns [barCount] amplitudes in 0..1
     * where bar[0] is the oldest slice in the window and bar[barCount-1] is the newest.
     * Amplitudes are RMS compressed to [shape] via a soft log curve so quiet speech still
     * shows up without clipping the peaks.
     */
    fun sampleFrame(
        timeSeconds: Float,
        barCount: Int,
        windowSpanSeconds: Float = 1.2f,
        shape: Float = 6f
    ): FloatArray {
        require(barCount > 0) { "barCount must be > 0" }
        val out = FloatArray(barCount)
        if (samples.isEmpty() || sampleRate <= 0) return out
        val samplesPerBar = ((windowSpanSeconds * sampleRate) / barCount).toInt().coerceAtLeast(1)
        val endSampleIdx = (timeSeconds * sampleRate).toInt()
        val startSampleIdx = endSampleIdx - samplesPerBar * barCount
        val logScale = ln(1f + shape)
        for (bi in 0 until barCount) {
            val bucketStart = startSampleIdx + bi * samplesPerBar
            val bucketEnd = bucketStart + samplesPerBar
            val s = bucketStart.coerceAtLeast(0)
            val e = bucketEnd.coerceAtMost(samples.size)
            var acc = 0.0
            var cnt = 0
            var i = s
            while (i < e) {
                val v = samples[i] / 32768.0
                acc += v * v
                cnt++
                i++
            }
            val rms = if (cnt > 0) sqrt(acc / cnt).toFloat() else 0f
            out[bi] = (ln(1f + shape * rms) / logScale).coerceIn(0f, 1f)
        }
        return out
    }

    /**
     * Pre-compute bars for every frame in a fixed-fps render. Memory budget is tiny
     * (barCount * fps * duration floats) — at 60 bars × 30 fps × 60 s that is ~400 KB.
     */
    fun sampleAllFrames(
        fps: Int,
        totalFrames: Int,
        barCount: Int,
        windowSpanSeconds: Float = 1.2f
    ): Array<FloatArray> {
        require(fps > 0 && totalFrames >= 0) { "fps and totalFrames must be valid" }
        val frames = Array(totalFrames) { FloatArray(barCount) }
        for (n in 0 until totalFrames) {
            val t = n.toFloat() / fps
            frames[n] = sampleFrame(t, barCount, windowSpanSeconds)
        }
        return frames
    }

    companion object {

        /**
         * Decoded PCM payload. We only support 16-bit PCM here — matches every WAV the
         * app produces (live recorder and server-cleaned outputs).
         */
        data class DecodedWav(val samples: ShortArray, val sampleRate: Int) {
            override fun equals(other: Any?): Boolean =
                other is DecodedWav && samples.contentEquals(other.samples) && sampleRate == other.sampleRate

            override fun hashCode(): Int = samples.contentHashCode() * 31 + sampleRate
        }

        /**
         * Parse a 16-bit PCM WAV file and return mono samples + sample rate. Multi-channel
         * input is downmixed to mono by averaging channels. Returns null if the file is
         * malformed or uses an unsupported codec (e.g. float PCM, ADPCM).
         */
        fun decodeWav(file: File): DecodedWav? {
            if (!file.exists() || file.length() < 44) return null
            val bytes = try { file.readBytes() } catch (_: Exception) { return null }
            if (bytes.size < 12) return null
            if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
                bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte()
            ) return null

            var offset = 12
            var dataStart = -1
            var dataSize = 0
            var sampleRate = 0
            var numChannels = 1
            var bitsPerSample = 16
            var audioFormat = 1

            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val payloadOffset = offset + 8
                when (chunkId) {
                    "fmt " -> {
                        if (payloadOffset + 16 > bytes.size) return null
                        val fmt = ByteBuffer.wrap(bytes, payloadOffset, chunkSize.coerceAtMost(32))
                            .order(ByteOrder.LITTLE_ENDIAN)
                        audioFormat = fmt.short.toInt() and 0xffff
                        numChannels = fmt.short.toInt() and 0xffff
                        sampleRate = fmt.int
                        fmt.int // byte rate
                        fmt.short // block align
                        bitsPerSample = fmt.short.toInt() and 0xffff
                    }
                    "data" -> {
                        dataStart = payloadOffset
                        dataSize = chunkSize.coerceAtMost(bytes.size - dataStart)
                    }
                }
                offset = payloadOffset + chunkSize + (chunkSize and 1)
                if (dataStart > 0 && sampleRate > 0) break
            }

            if (dataStart < 0 || sampleRate <= 0 || numChannels < 1) return null
            if (audioFormat != 1 || bitsPerSample != 16) return null

            val totalShorts = dataSize / 2
            if (totalShorts <= 0) return null
            val frames = totalShorts / numChannels
            val mono = ShortArray(frames)
            val bb = ByteBuffer.wrap(bytes, dataStart, dataSize).order(ByteOrder.LITTLE_ENDIAN)
            if (numChannels == 1) {
                for (i in 0 until frames) mono[i] = bb.short
            } else {
                for (i in 0 until frames) {
                    var acc = 0
                    for (c in 0 until numChannels) acc += bb.short.toInt()
                    mono[i] = (acc / numChannels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            return DecodedWav(mono, sampleRate)
        }
    }
}
