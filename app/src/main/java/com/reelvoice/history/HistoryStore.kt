package com.reelvoice.history

import android.content.Context
import com.reelvoice.ui.screens.AudioPair
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Disk-backed persistence for the History / Recent Cleans list.
 *
 * [AudioPair] used to live only in an in-memory list, so every processed audio/video output
 * vanished on app restart. This store serializes the list to a small JSON file in the app's
 * private files dir and restores it on launch, so video (and audio) outputs actually appear
 * in History across sessions.
 */
class HistoryStore(context: Context) {

    private val file = File(context.filesDir, "reelvoice_history.json")

    /** Load the persisted pairs, newest first. Returns an empty list on any read/parse failure. */
    fun load(): MutableList<AudioPair> {
        if (!file.exists()) return mutableListOf()
        return try {
            val array = JSONArray(file.readText())
            val out = ArrayList<AudioPair>(array.length())
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val cleaned = o.optString("cleanedFile")
                if (cleaned.isEmpty()) continue
                out.add(
                    AudioPair(
                        timestamp = o.optString("timestamp"),
                        time = o.optString("time"),
                        originalFile = o.optString("originalFile"),
                        cleanedFile = cleaned,
                        isRecording = o.optBoolean("isRecording", false),
                        durationSec = o.optDouble("durationSec", 0.0).toFloat()
                    )
                )
            }
            out
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    /** Persist the given pairs (newest first). Failures are swallowed — history is best-effort. */
    fun save(pairs: List<AudioPair>) {
        try {
            val array = JSONArray()
            pairs.take(MAX_ITEMS).forEach { p ->
                array.put(
                    JSONObject().apply {
                        put("timestamp", p.timestamp)
                        put("time", p.time)
                        put("originalFile", p.originalFile)
                        put("cleanedFile", p.cleanedFile)
                        put("isRecording", p.isRecording)
                        put("durationSec", p.durationSec.toDouble())
                    }
                )
            }
            file.writeText(array.toString())
        } catch (_: Exception) {
            // Best-effort persistence; never crash the app over history.
        }
    }

    companion object {
        const val MAX_ITEMS = 50
    }
}
