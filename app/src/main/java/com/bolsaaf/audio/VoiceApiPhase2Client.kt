package com.bolsaaf.audio

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class VoiceJobAccepted(
    val jobId: Int,
    val jobType: String,
    val message: String
)

data class VoiceJobStatus(
    val jobId: Int,
    val status: String,
    val processingMode: String?,
    val outputAudioUrl: String?,
    val outputVideoUrl: String?,
    val cleanedUrl: String?,
    val errorMessage: String?
)

data class VoiceBackground(
    val id: String,
    val label: String,
    val previewUrl: String?,
    val defaultVolume: Float
)

/**
 * Contract-first client for upcoming phase-2+ async endpoints.
 * This is scaffold-safe and can be integrated screen-by-screen.
 */
class VoiceApiPhase2Client(
    private val baseUrl: String = "https://shadowselfwork.com/voice/"
) {
    companion object {
        private const val TAG = "VoiceApiPhase2Client"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
    }

    private val siteOrigin: String = run {
        val u = URL(baseUrl.trimEnd('/') + "/")
        val port = when {
            u.port == -1 -> ""
            u.port == u.defaultPort -> ""
            else -> ":${u.port}"
        }
        "${u.protocol}://${u.host}$port"
    }

    fun extractVoice(file: File, mode: String = "studio", outputFormat: String = "wav"): VoiceJobAccepted {
        return submitJob(
            endpoint = "extract_voice/",
            file = file,
            fields = mapOf("mode" to mode, "output_format" to outputFormat)
        )
    }

    /**
     * Async job: server downloads audio from a public URL (YouTube / Reels / TikTok),
     * then runs the same vocal extract + clean pipeline. Requires matching Django route.
     */
    fun extractVoiceFromUrl(sourceUrl: String, mode: String = "studio"): VoiceJobAccepted {
        val url = baseUrl.trimEnd('/') + "/extract_from_url/"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.doOutput = true
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        val payload = JSONObject()
            .put("source_url", sourceUrl.trim())
            .put("mode", mode)
            .toString()
        conn.outputStream.use { out ->
            out.write(payload.toByteArray(Charsets.UTF_8))
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Log.d(TAG, "extract_from_url code=$code body=${body.take(400)}")
        if (code !in 200..299) throw VoiceCleaningException("extract_from_url failed: HTTP $code $body")
        val j = JSONObject(body)
        return VoiceJobAccepted(
            jobId = j.optInt("job_id"),
            jobType = j.optString("job_type", "extract_from_url"),
            message = j.optString("message", "Job accepted")
        )
    }

    fun addBackground(
        file: File,
        backgroundId: String,
        bgVolume: Float = 0.15f,
        mode: String = "standard"
    ): VoiceJobAccepted {
        return submitJob(
            endpoint = "add_background/",
            file = file,
            fields = mapOf(
                "bg" to backgroundId,
                "bg_volume" to bgVolume.toString(),
                "mode" to mode
            )
        )
    }

    fun reelMode(
        file: File,
        mode: String = "standard",
        backgroundId: String? = null,
        bgVolume: Float = 0.15f
    ): VoiceJobAccepted {
        val fields = mutableMapOf("mode" to mode, "bg_volume" to bgVolume.toString())
        if (!backgroundId.isNullOrBlank()) fields["bg"] = backgroundId
        return submitJob(endpoint = "reel/", file = file, fields = fields)
    }

    fun processVideo(file: File, jobType: String = "video_reel", mode: String = "standard"): VoiceJobAccepted {
        return submitJob(
            endpoint = "video/process/",
            file = file,
            fields = mapOf("job_type" to jobType, "mode" to mode)
        )
    }

    fun getBackgrounds(): List<VoiceBackground> {
        val endpoint = baseUrl.trimEnd('/') + "/backgrounds/"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw VoiceCleaningException("Background list failed: HTTP $code $body")
        val json = JSONObject(body)
        val arr = json.optJSONArray("backgrounds") ?: JSONArray()
        val list = ArrayList<VoiceBackground>()
        for (i in 0 until arr.length()) {
            val j = arr.optJSONObject(i) ?: continue
            list.add(
                VoiceBackground(
                    id = j.optString("id"),
                    label = j.optString("label"),
                    previewUrl = resolveUrlNullable(j.optString("preview_url")),
                    defaultVolume = j.optDouble("default_volume", 0.15).toFloat()
                )
            )
        }
        return list
    }

    fun getStatus(jobId: Int): VoiceJobStatus {
        val endpoint = baseUrl.trimEnd('/') + "/status/$jobId/"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        val code = conn.responseCode
        val stream: InputStream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw VoiceCleaningException("Status failed: HTTP $code $body")
        val j = JSONObject(body)
        return VoiceJobStatus(
            jobId = j.optInt("job_id", jobId),
            status = j.optString("status", "unknown"),
            processingMode = j.optString("processing_mode").ifBlank {
                j.optString("mode").takeIf { it.isNotBlank() } ?: ""
            }.ifBlank { null },
            outputAudioUrl = resolveUrlNullable(j.optString("output_audio_url")),
            outputVideoUrl = resolveUrlNullable(j.optString("output_video_url")),
            cleanedUrl = resolveUrlNullable(j.optString("cleaned_url")),
            errorMessage = j.optString("error_message").ifBlank { null }
        )
    }

    private fun submitJob(endpoint: String, file: File, fields: Map<String, String>): VoiceJobAccepted {
        require(file.exists()) { "Input file does not exist: ${file.absolutePath}" }
        val url = baseUrl.trimEnd('/') + "/" + endpoint.trimStart('/')
        val boundary = "----WebKitFormBoundary${UUID.randomUUID()}"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        conn.doOutput = true
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS

        conn.outputStream.use { out ->
            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write(
                "Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n".toByteArray(Charsets.UTF_8)
            )
            out.write("Content-Type: application/octet-stream\r\n\r\n".toByteArray(Charsets.UTF_8))
            file.inputStream().use { it.copyTo(out) }
            out.write("\r\n".toByteArray(Charsets.UTF_8))
            for ((k, v) in fields) {
                out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                out.write("Content-Disposition: form-data; name=\"$k\"\r\n\r\n".toByteArray(Charsets.UTF_8))
                out.write(v.toByteArray(Charsets.UTF_8))
                out.write("\r\n".toByteArray(Charsets.UTF_8))
            }
            out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Log.d(TAG, "submitJob endpoint=$endpoint code=$code body=${body.take(240)}")
        if (code !in 200..299) throw VoiceCleaningException("Submit job failed: HTTP $code $body")
        val j = JSONObject(body)
        return VoiceJobAccepted(
            jobId = j.optInt("job_id"),
            jobType = j.optString("job_type", endpoint.trim('/')),
            message = j.optString("message", "Job accepted")
        )
    }

    private fun resolveUrlNullable(pathOrUrl: String?): String? {
        val p = pathOrUrl?.trim().orEmpty()
        if (p.isEmpty() || p == "null") return null
        if (p.startsWith("http://", true) || p.startsWith("https://", true)) return p
        if (p.startsWith("/")) return siteOrigin + p
        return "$siteOrigin/$p"
    }
}
