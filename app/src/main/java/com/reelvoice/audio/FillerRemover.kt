package com.reelvoice.audio

import kotlin.math.sqrt

/**
 * Acoustic filler-word remover. Works on 16-bit mono PCM, no ASR.
 *
 * Heuristic: a "filler" is a short speech island (≤ [Config.fillerMaxMs])
 * bracketed by silences of at least [Config.contextSilenceMinMs] on both
 * sides. That shape matches how an "umm" / "uhh" / isolated "matlab" sits in
 * a recording — short, vocalised, buffered by pauses. It's a BETA detector:
 * no transcription, so it will occasionally trim real short words. We
 * deliberately leave the head and tail speech islands alone so intros/outros
 * never get eaten.
 */
object FillerRemover {

    data class Config(
        val rmsThreshold: Float = 0.015f,
        val fillerMaxMs: Int = 350,
        val contextSilenceMinMs: Int = 180,
        val windowMs: Int = 20,
    )

    class Report(
        val output: ShortArray,
        val originalDurationSec: Float,
        val outputDurationSec: Float,
        val fillersRemoved: Int,
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
        if (numWindows < 3) {
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

        // Split into alternating silent + speech runs (window ranges).
        data class Run(val isSilent: Boolean, val startWin: Int, val endWin: Int) {
            val lenWin: Int get() = endWin - startWin + 1
        }
        val runs = mutableListOf<Run>()
        var rStart = 0
        var rSilent = silent[0]
        for (w in 1 until numWindows) {
            if (silent[w] != rSilent) {
                runs += Run(rSilent, rStart, w - 1)
                rStart = w
                rSilent = silent[w]
            }
        }
        runs += Run(rSilent, rStart, numWindows - 1)

        val fillerMaxWin = (config.fillerMaxMs / config.windowMs).coerceAtLeast(1)
        val contextMinWin = (config.contextSilenceMinMs / config.windowMs).coerceAtLeast(1)

        // Flag speech runs as filler when short and flanked by long silences.
        // Never flag the first or last speech island (keeps head/tail intact).
        val fillerRunIndices = mutableListOf<Int>()
        val speechIdxs = runs.indices.filter { !runs[it].isSilent }
        if (speechIdxs.size <= 2) {
            return Report(input, origDur, origDur, 0, 0)
        }
        for (i in speechIdxs) {
            if (i == speechIdxs.first() || i == speechIdxs.last()) continue
            val run = runs[i]
            if (run.lenWin > fillerMaxWin) continue
            val leftSilent = runs.getOrNull(i - 1)?.takeIf { it.isSilent }
            val rightSilent = runs.getOrNull(i + 1)?.takeIf { it.isSilent }
            if (leftSilent == null || rightSilent == null) continue
            if (leftSilent.lenWin < contextMinWin) continue
            if (rightSilent.lenWin < contextMinWin) continue
            fillerRunIndices += i
        }
        if (fillerRunIndices.isEmpty()) {
            return Report(input, origDur, origDur, 0, 0)
        }

        // Build trim ranges in sample space, including the flanking half-silence
        // on each side so we don't leave an obvious gap where the filler used to be.
        data class TrimRange(val startSample: Int, val endSample: Int)
        val trims = mutableListOf<TrimRange>()
        for (ri in fillerRunIndices) {
            val run = runs[ri]
            val left = runs[ri - 1]
            val right = runs[ri + 1]
            val leftTrimWin = (left.lenWin / 2).coerceAtLeast(0)
            val rightTrimWin = (right.lenWin / 2).coerceAtLeast(0)
            val startWin = (run.startWin - leftTrimWin).coerceAtLeast(0)
            val endWinExclusive = (run.endWin + 1 + rightTrimWin).coerceAtMost(numWindows)
            val startSample = startWin * windowSize
            val endSample = (endWinExclusive * windowSize).coerceAtMost(n)
            if (endSample > startSample) trims += TrimRange(startSample, endSample)
        }

        if (trims.isEmpty()) {
            return Report(input, origDur, origDur, 0, 0)
        }

        val totalTrim = trims.sumOf { it.endSample - it.startSample }
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
        }

        return Report(
            output = output,
            originalDurationSec = origDur,
            outputDurationSec = output.size.toFloat() / sampleRate,
            fillersRemoved = fillerRunIndices.size,
            samplesRemoved = totalTrim,
        )
    }
}
