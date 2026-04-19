package com.reelvoice.audio

import android.content.Context
import java.util.Locale

/**
 * Rolling "noise frustration" score from in-app feedback. Biases the next adaptive analysis
 * toward stronger cloud modes / denoise when users report residual noise.
 */
object FeedbackAdaptiveMemory {

    private const val PREF = "reelvoice_feedback_learning"
    private const val KEY_NOISE_SCORE = "noise_score"

    fun record(context: Context, clearVoice: Boolean, issueType: String?, notes: String?) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (clearVoice) {
            val v = (sp.getInt(KEY_NOISE_SCORE, 0) - 1).coerceAtLeast(0)
            sp.edit().putInt(KEY_NOISE_SCORE, v).apply()
            return
        }
        val it = issueType?.trim().orEmpty()
        val n = notes?.trim().orEmpty()
        val blob = "$it $n".lowercase(Locale.US)
        val strongNoise =
            it.contains("noise", ignoreCase = true) ||
                it.contains("bacha", ignoreCase = true) ||
                blob.contains("noise") ||
                blob.contains("hiss") ||
                blob.contains("background") ||
                blob.contains("market") ||
                blob.contains("crowd") ||
                blob.contains("ambient")
        val delta = if (strongNoise) 3 else 1
        val v = (sp.getInt(KEY_NOISE_SCORE, 0) + delta).coerceIn(0, 24)
        sp.edit().putInt(KEY_NOISE_SCORE, v).apply()
    }

    private fun boostSteps(context: Context): Int {
        val s = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_NOISE_SCORE, 0)
        return when {
            s >= 9 -> 3
            s >= 5 -> 2
            s >= 2 -> 1
            else -> 0
        }
    }

    fun applyFromFeedbackHistory(
        context: Context,
        profile: AdaptiveAudioAnalyzer.Profile
    ): AdaptiveAudioAnalyzer.Profile {
        val steps = boostSteps(context)
        if (steps == 0) return profile

        val order = listOf("basic", "standard", "studio", "pro")
        val cur = profile.adaptivePreset.mode.lowercase(Locale.ROOT)
        var idx = order.indexOf(cur)
        if (idx < 0) idx = order.indexOf("standard").coerceAtLeast(0)
        val newMode = order[(idx + steps).coerceAtMost(order.lastIndex)]

        val newDenoise = when (steps) {
            3 -> "STRONG"
            2 -> bumpDenoise(profile.adaptivePreset.denoiseLevel, "MEDIUM")
            else -> bumpDenoise(profile.adaptivePreset.denoiseLevel, "MEDIUM")
        }
        val newDry = (profile.adaptivePreset.dryMix - 0.03 * steps).coerceAtLeast(0.04)
        val preset = profile.adaptivePreset.copy(
            mode = newMode,
            denoiseLevel = newDenoise,
            dryMix = newDry
        )
        val flags = profile.flags + listOf("feedback_noise_boost_$steps")
        return profile.copy(
            adaptivePreset = preset,
            suggestedCleaningPreset = preset.toCleaningPreset(),
            suggestedCloudMode = newMode,
            flags = flags
        )
    }

    private fun bumpDenoise(current: String, floor: String): String {
        val order = listOf("LIGHT", "MEDIUM", "STRONG")
        val i = order.indexOf(current).takeIf { it >= 0 } ?: 1
        val j = order.indexOf(floor).takeIf { it >= 0 } ?: 1
        return order[i.coerceAtLeast(j).coerceAtMost(order.lastIndex)]
    }
}
