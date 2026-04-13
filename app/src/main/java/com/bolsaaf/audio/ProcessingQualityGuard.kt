package com.bolsaaf.audio

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Phase 5 — Advanced: compare original vs processed PCM to flag over-suppression / suspicious output.
 * Does not modify audio; callers may log, downgrade mode on retry, or show UI.
 */
object ProcessingQualityGuard {

    data class Report(
        val pass: Boolean,
        val rmsDeltaDb: Float,
        val peakDeltaDb: Float,
        val rmsOutDbfs: Float,
        val nearSilenceOutput: Boolean,
        val issues: List<String>
    )

    private const val FULL_SCALE = 32768f

    fun compare(original: ShortArray, cleaned: ShortArray): Report {
        if (original.isEmpty() || cleaned.isEmpty()) {
            return Report(
                pass = false,
                rmsDeltaDb = 0f,
                peakDeltaDb = 0f,
                rmsOutDbfs = -120f,
                nearSilenceOutput = true,
                issues = listOf("empty_buffer")
            )
        }
        val n = minOf(original.size, cleaned.size)
        var sumO = 0.0
        var sumC = 0.0
        var peakO = 0
        var peakC = 0
        for (i in 0 until n) {
            val o = original[i].toInt()
            val c = cleaned[i].toInt()
            sumO += (o * o).toDouble()
            sumC += (c * c).toDouble()
            peakO = maxOf(peakO, kotlin.math.abs(o))
            peakC = maxOf(peakC, kotlin.math.abs(c))
        }
        val rmsO = sqrt(sumO / n).toFloat().coerceAtLeast(1e-8f)
        val rmsC = sqrt(sumC / n).toFloat().coerceAtLeast(1e-12f)
        val rmsDeltaDb = 20f * log10(rmsC / rmsO)
        val peakDeltaDb = 20f * log10((peakC.coerceAtLeast(1).toFloat()) / peakO.coerceAtLeast(1).toFloat())

        val rmsOutDbfs = 20f * log10(rmsC / FULL_SCALE).coerceIn(-120f, 0f)
        val nearSilence = rmsOutDbfs < -55f

        val issues = ArrayList<String>()
        if (rmsDeltaDb < -8f) issues.add("heavy_rms_drop")
        if (peakDeltaDb < -10f && peakO > 1000) issues.add("peak_collapsed")
        if (nearSilence) issues.add("output_very_quiet")

        val pass = issues.isEmpty()
        return Report(
            pass = pass,
            rmsDeltaDb = rmsDeltaDb,
            peakDeltaDb = peakDeltaDb,
            rmsOutDbfs = rmsOutDbfs,
            nearSilenceOutput = nearSilence,
            issues = issues
        )
    }
}
