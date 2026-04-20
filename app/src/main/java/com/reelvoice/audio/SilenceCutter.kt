package com.reelvoice.audio

import kotlin.math.sqrt

/**
 * Detects pauses longer than [Config.minPauseSec] in 16-bit mono PCM via an
 * RMS threshold, and trims the middle of each pause — keeping a small natural
 * gap (the first and last half of [Config.keepPauseSec]) so the splice still
 * *feels* like a pause instead of a hard cut.
 *
 * Splicing happens inside already-silent zones, so there's no click-fade
 * needed for MVP. Pure Kotlin — no Android dependencies, JVM testable.
 */
object SilenceCutter {

    data class Config(
        /** Normalized RMS below which a window is considered silent (0..1). */
        val rmsThreshold: Float = 0.015f,
        /** Only pauses at least this long get trimmed. */
        val minPauseSec: Float = 0.5f,
        /** How much of the original pause to preserve around the splice. */
        val keepPauseSec: Float = 0.18f,
        /** Analysis window length in ms — shorter = tighter boundary detection. */
        val windowMs: Int = 20,
    )

    class Report(
        val output: ShortArray,
        val originalDurationSec: Float,
        val outputDurationSec: Float,
        val regionsTrimmed: Int,
        val samplesRemoved: Int,
    ) {
        val secondsRemoved: Float
            get() = originalDurationSec - outputDurationSec
    }

    fun process(
        input: ShortArray,
        sampleRate: Int,
        config: Config = Config()
    ): Report {
        val n = input.size
        val origDur = if (sampleRate > 0) n.toFloat() / sampleRate else 0f
        if (n == 0 || sampleRate <= 0) {
            return Report(input, origDur, origDur, 0, 0)
        }

        val windowSize = ((config.windowMs * sampleRate) / 1000).coerceAtLeast(1)
        val numWindows = n / windowSize
        if (numWindows == 0) {
            return Report(input, origDur, origDur, 0, 0)
        }

        val silent = BooleanArray(numWindows)
        for (w in 0 until numWindows) {
            val start = w * windowSize
            var acc = 0.0
            for (i in 0 until windowSize) {
                val v = input[start + i] / 32768.0
                acc += v * v
            }
            val rms = sqrt(acc / windowSize).toFloat()
            silent[w] = rms < config.rmsThreshold
        }

        val regions = mutableListOf<IntRange>()
        var rs = -1
        for (w in 0 until numWindows) {
            if (silent[w]) {
                if (rs < 0) rs = w
            } else if (rs >= 0) {
                regions.add(rs..(w - 1))
                rs = -1
            }
        }
        if (rs >= 0) regions.add(rs..(numWindows - 1))

        val minPauseWindows = (config.minPauseSec * 1000f / config.windowMs).toInt().coerceAtLeast(1)
        val halfKeepWindows = (config.keepPauseSec * 1000f / (2 * config.windowMs)).toInt().coerceAtLeast(0)

        data class TrimRange(val startSample: Int, val endSample: Int)

        val trims = mutableListOf<TrimRange>()
        for (region in regions) {
            val len = region.last - region.first + 1
            if (len <= minPauseWindows) continue
            val trimStartWin = region.first + halfKeepWindows
            val trimEndWin = region.last - halfKeepWindows
            if (trimEndWin < trimStartWin) continue
            val startSample = trimStartWin * windowSize
            val endSample = ((trimEndWin + 1) * windowSize).coerceAtMost(n)
            if (endSample > startSample) {
                trims += TrimRange(startSample, endSample)
            }
        }

        val totalTrim = trims.sumOf { it.endSample - it.startSample }
        if (totalTrim == 0) {
            return Report(input, origDur, origDur, 0, 0)
        }

        val output = ShortArray(n - totalTrim)
        var src = 0
        var dst = 0
        for (range in trims) {
            val keep = range.startSample - src
            if (keep > 0) {
                System.arraycopy(input, src, output, dst, keep)
                dst += keep
            }
            src = range.endSample
        }
        val tail = n - src
        if (tail > 0) {
            System.arraycopy(input, src, output, dst, tail)
            dst += tail
        }

        return Report(
            output = output,
            originalDurationSec = origDur,
            outputDurationSec = output.size.toFloat() / sampleRate,
            regionsTrimmed = trims.size,
            samplesRemoved = totalTrim,
        )
    }
}
