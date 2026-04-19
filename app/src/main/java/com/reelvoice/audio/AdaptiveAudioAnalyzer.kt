package com.reelvoice.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Phase 3 — Smart processing: inspect PCM (48k mono s16le) and auto-select best runtime preset.
 */

fun ByteArray.pcm16LeToShortArray(): ShortArray {
    require(size % 2 == 0) { "PCM byte length must be even" }
    val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    val out = ShortArray(buf.remaining())
    buf.get(out)
    return out
}

object AdaptiveAudioAnalyzer {

    private const val FULL_SCALE = 32768f
    private const val MAX_ANALYSIS_SECONDS = 20

    data class AdaptivePreset(
        val preGain: Double,
        val denoiseLevel: String,
        val compressorStrength: String,
        val dryMix: Double,
        val mode: String
    ) {
        fun toCleaningPreset(): CleaningPreset = when {
            mode == "studio" && denoiseLevel == "STRONG" -> CleaningPreset.STUDIO
            denoiseLevel == "STRONG" -> CleaningPreset.STRONG
            else -> CleaningPreset.NORMAL
        }
    }

    data class Profile(
        val rmsDbfs: Float,
        val peakDbfs: Float,
        val crestFactorDb: Float,
        val zeroSampleFraction: Float,
        val nearZeroFraction: Float,
        val nearClipFraction: Float,
        val quietFrameFraction: Float,
        val adaptivePreset: AdaptivePreset,
        val suggestedCleaningPreset: CleaningPreset,
        val suggestedCloudMode: String,
        val flags: List<String>,
        val confidence: Float
    ) {
        companion object {
            fun empty() = Profile(
                rmsDbfs = -120f,
                peakDbfs = -120f,
                crestFactorDb = 0f,
                zeroSampleFraction = 0f,
                nearZeroFraction = 0f,
                nearClipFraction = 0f,
                quietFrameFraction = 0f,
                adaptivePreset = AdaptivePreset(2.5, "MEDIUM", "MEDIUM", 0.1, "standard"),
                suggestedCleaningPreset = CleaningPreset.NORMAL,
                suggestedCloudMode = "standard",
                flags = listOf("empty_signal"),
                confidence = 0f
            )
        }

        fun uiHeadline(): String {
            val pct = (confidence * 100f).toInt().coerceIn(0, 100)
            return "Suggested: ${suggestedCleaningPreset.label} mode ($pct% confident)"
        }
    }

    fun analyze(samples: ShortArray, sampleRate: Int): Profile {
        if (samples.isEmpty()) return Profile.empty()
        val maxSamples = max(1, sampleRate * MAX_ANALYSIS_SECONDS)
        val window = if (samples.size <= maxSamples) samples else samples.copyOfRange(0, maxSamples)

        var sumSq = 0.0
        var peak = 0
        var zeros = 0
        var nearZero = 0
        var nearClip = 0
        for (s in window) {
            val v = s.toInt()
            val av = kotlin.math.abs(v)
            sumSq += (v * v).toDouble()
            if (av > peak) peak = av
            if (v == 0) zeros++
            if (av <= 8) nearZero++
            if (av >= 31000) nearClip++
        }
        val n = window.size.toFloat().coerceAtLeast(1f)
        val rms = sqrt(sumSq / n).toFloat()
        val rmsDbfs = linearToDbfs(rms)
        val peakDbfs = linearToDbfs(peak.toFloat())
        val crest = if (rms > 1e-6f) peak.toFloat() / rms else peak.toFloat()
        val crestDb = 20f * log10(crest.coerceAtLeast(1e-6f))
        val zeroFrac = zeros / n
        val nearZeroFrac = nearZero / n
        val clipFrac = nearClip / n

        val frameLen = max(sampleRate / 50, 160)
        var quietFrames = 0
        var frameCount = 0
        var i = 0
        while (i + frameLen <= window.size) {
            var fs = 0.0
            for (j in i until i + frameLen) {
                val v = window[j].toInt()
                fs += (v * v).toDouble()
            }
            val fr = sqrt(fs / frameLen).toFloat()
            if (linearToDbfs(fr) < -50f) quietFrames++
            frameCount++
            i += frameLen
        }
        val quietFrameFrac = if (frameCount > 0) quietFrames.toFloat() / frameCount else 0f

        val nearZeroPct = nearZeroFrac * 100f
        val zeroPct = zeroFrac * 100f

        var preGain = 2.5
        var denoise = "MEDIUM"
        var compressor = "MEDIUM"
        var dryMix = 0.10
        var mode = "standard"
        val flags = ArrayList<String>()

        // Loudness (RMS based)
        when {
            rmsDbfs < -60f -> {
                preGain = 4.0
                denoise = "STRONG"
                mode = "studio"
                flags.add("very_low_rms")
            }
            rmsDbfs > -45f -> {
                preGain = 1.5
                denoise = "LIGHT"
                mode = "standard"
                flags.add("loud_input")
            }
            else -> {
                preGain = 2.5
                denoise = "MEDIUM"
                mode = "standard"
                flags.add("normal_rms")
            }
        }

        // Noise (near-zero based)
        when {
            nearZeroPct > 65f -> {
                denoise = "STRONG"
                flags.add("high_noise_near_zero")
            }
            nearZeroPct > 40f -> {
                denoise = "MEDIUM"
                flags.add("moderate_noise_near_zero")
            }
            else -> {
                denoise = "LIGHT"
                flags.add("cleaner_input")
            }
        }

        // Clipping guard
        if (peakDbfs > -3f) {
            preGain -= 1.0
            compressor = "STRONG"
            flags.add("clipping_risk")
        }

        // Hollow output risk
        if (zeroPct > 40f) {
            dryMix = 0.15
            flags.add("hollow_risk_zero")
        }

        preGain = preGain.coerceIn(0.8, 6.0)

        val adaptivePreset = AdaptivePreset(
            preGain = preGain,
            denoiseLevel = denoise,
            compressorStrength = compressor,
            dryMix = dryMix,
            mode = mode
        )

        val suggestedPreset = adaptivePreset.toCleaningPreset()
        val confidence = computeConfidence(rmsDbfs, peakDbfs, nearZeroPct, zeroPct, flags)

        return Profile(
            rmsDbfs = rmsDbfs,
            peakDbfs = peakDbfs,
            crestFactorDb = crestDb,
            zeroSampleFraction = zeroFrac,
            nearZeroFraction = nearZeroFrac,
            nearClipFraction = clipFrac,
            quietFrameFraction = quietFrameFrac,
            adaptivePreset = adaptivePreset,
            suggestedCleaningPreset = suggestedPreset,
            suggestedCloudMode = mode,
            flags = flags,
            confidence = confidence
        )
    }

    private fun computeConfidence(
        rmsDbfs: Float,
        peakDbfs: Float,
        nearZeroPct: Float,
        zeroPct: Float,
        flags: List<String>
    ): Float {
        var c = 0.48f
        if (rmsDbfs < -60f || rmsDbfs > -45f) c += 0.16f
        if (nearZeroPct > 65f || nearZeroPct < 25f) c += 0.14f
        if (peakDbfs > -3f) c += 0.12f
        if (zeroPct > 40f) c += 0.08f
        c += (flags.size.coerceAtMost(4) * 0.04f)
        return c.coerceIn(0.35f, 0.96f)
    }

    private fun linearToDbfs(linear: Float): Float {
        if (linear <= 1e-12f) return -120f
        return (20f * log10(linear / FULL_SCALE)).coerceIn(-120f, 0f)
    }
}
