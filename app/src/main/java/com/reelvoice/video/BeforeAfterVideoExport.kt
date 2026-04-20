package com.reelvoice.video

import java.io.File

/**
 * Builds a ~6-second "Before / After" MP4: first half plays the noisy
 * original while the top waveform pulses; second half plays the cleaned
 * output while the bottom waveform pulses. Uses [WaveformVideoEncoder]
 * unchanged, wired with a [BeforeAfterSampler] + [BeforeAfterFrameRenderer].
 *
 * Call off the main thread — encode time is roughly 0.5×–1.5× of the
 * output duration depending on device.
 */
object BeforeAfterVideoExport {

    /** Seconds of each side (before / after). Total clip = 2 × this. */
    const val SEGMENT_SEC: Float = 3f

    fun generate(
        originalWav: File,
        cleanedWav: File,
        output: File,
        onProgress: (Float) -> Unit = {}
    ): File {
        val original = WaveformWindowSampler.decodeWav(originalWav)
            ?: error("Couldn't read original WAV (16-bit PCM only)")
        val cleaned = WaveformWindowSampler.decodeWav(cleanedWav)
            ?: error("Couldn't read cleaned WAV (16-bit PCM only)")
        if (original.sampleRate != cleaned.sampleRate) {
            error("Sample rates don't match (${original.sampleRate} vs ${cleaned.sampleRate})")
        }
        val sampleRate = original.sampleRate
        val segmentSamples = (SEGMENT_SEC * sampleRate).toInt()

        val originalTrim = original.samples.takeFirst(segmentSamples)
        val cleanedTrim = cleaned.samples.takeFirst(segmentSamples)

        val sampler = BeforeAfterSampler(
            originalSamples = originalTrim,
            cleanedSamples = cleanedTrim,
            sampleRate = sampleRate
        )
        val renderer = BeforeAfterFrameRenderer(
            width = WaveformVideoEncoder.DEFAULT_WIDTH,
            height = WaveformVideoEncoder.DEFAULT_HEIGHT
        )
        val encoder = WaveformVideoEncoder(
            sampler = sampler,
            renderer = renderer,
            barCount = BAR_COUNT_TOTAL
        )
        output.parentFile?.mkdirs()
        encoder.encode(output) { frac -> onProgress(frac) }
        return output
    }

    /** Total bars across both panes — sampler splits into 2 equal halves. */
    private const val BAR_COUNT_TOTAL = 60

    private fun ShortArray.takeFirst(n: Int): ShortArray {
        if (n >= size) return this
        val out = ShortArray(n)
        System.arraycopy(this, 0, out, 0, n)
        return out
    }
}
