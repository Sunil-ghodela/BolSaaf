package com.bolsaaf.audio

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
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

data class ReelV2VariantOutput(
    val audioUrl: String?,
    val videoUrl: String?,
    val durationSec: Float?,
    val loudnessLufs: Float?
)

data class ReelV2JobStatus(
    val reelJobId: Int,
    val status: String,
    val currentStage: String?,
    val overallProgress: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val outputs: Map<String, ReelV2VariantOutput>
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

        /**
         * Server max upload size for audio clean flows. Server will 413 above this, so
         * callers should pre-flight check before hitting the network.
         */
        const val MAX_AUDIO_UPLOAD_BYTES = 5L * 1024L * 1024L
    }

    private inline fun <T> runNetwork(operation: String, block: () -> T): T {
        return try {
            block()
        } catch (e: VoiceCleaningException) {
            throw e
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "$operation timed out", e)
            throw VoiceCleaningException("Request timed out — please check your connection and retry.", e)
        } catch (e: UnknownHostException) {
            Log.w(TAG, "$operation failed to resolve host", e)
            throw VoiceCleaningException("You appear to be offline — please check your connection.", e)
        } catch (e: IOException) {
            Log.w(TAG, "$operation network error", e)
            throw VoiceCleaningException("Network error — please try again in a moment.", e)
        } catch (e: JSONException) {
            Log.w(TAG, "$operation got malformed server response", e)
            throw VoiceCleaningException("Server returned an unexpected response. Please try again.", e)
        }
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
    fun extractVoiceFromUrl(sourceUrl: String, mode: String = "studio"): VoiceJobAccepted = runNetwork("extractVoiceFromUrl") {
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
        if (code !in 200..299) throw VoiceCleaningException(friendlyHttpError(code, body))
        val j = JSONObject(body)
        VoiceJobAccepted(
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

    fun createReelV2(
        file: File,
        requestedVariants: List<String> = listOf("clean_only", "with_bg", "viral_boosted"),
        targetLufs: Float = -16f,
        includeVideo: Boolean = false,
        backgroundPreset: String? = null
    ): VoiceJobAccepted = runNetwork("createReelV2") {
        require(file.exists()) { "Input file does not exist: ${file.absolutePath}" }
        val endpoint = baseUrl.trimEnd('/') + "/reel/create/"
        val boundary = "----WebKitFormBoundary${UUID.randomUUID()}"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
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

            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write("Content-Disposition: form-data; name=\"requested_variants\"\r\n\r\n".toByteArray(Charsets.UTF_8))
            out.write(requestedVariants.joinToString(",").toByteArray(Charsets.UTF_8))
            out.write("\r\n".toByteArray(Charsets.UTF_8))

            if (!backgroundPreset.isNullOrBlank()) {
                out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                out.write("Content-Disposition: form-data; name=\"background_preset\"\r\n\r\n".toByteArray(Charsets.UTF_8))
                out.write(backgroundPreset.toByteArray(Charsets.UTF_8))
                out.write("\r\n".toByteArray(Charsets.UTF_8))
            }

            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write("Content-Disposition: form-data; name=\"target_lufs\"\r\n\r\n".toByteArray(Charsets.UTF_8))
            out.write(targetLufs.toString().toByteArray(Charsets.UTF_8))
            out.write("\r\n".toByteArray(Charsets.UTF_8))

            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write("Content-Disposition: form-data; name=\"include_video\"\r\n\r\n".toByteArray(Charsets.UTF_8))
            out.write(includeVideo.toString().toByteArray(Charsets.UTF_8))
            out.write("\r\n".toByteArray(Charsets.UTF_8))

            out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Log.d(TAG, "createReelV2 code=$code body=${body.take(300)}")
        if (code !in 200..299) throw VoiceCleaningException(friendlyHttpError(code, body))
        val j = JSONObject(body)
        VoiceJobAccepted(
            jobId = j.optInt("reel_job_id"),
            jobType = "reel_v2",
            message = j.optString("message", "Job accepted")
        )
    }

    fun getReelV2Status(reelJobId: Int): ReelV2JobStatus = runNetwork("getReelV2Status") {
        val endpoint = baseUrl.trimEnd('/') + "/reel/$reelJobId/status/"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw VoiceCleaningException(friendlyHttpError(code, body))
        val j = JSONObject(body)

        val outputsObj = j.optJSONObject("outputs")
        val outputs = linkedMapOf<String, ReelV2VariantOutput>()
        listOf("clean_only", "with_bg", "viral_boosted").forEach { key ->
            val o = outputsObj?.optJSONObject(key)
            outputs[key] = ReelV2VariantOutput(
                audioUrl = resolveUrlNullable(o?.optString("audio_url")),
                videoUrl = resolveUrlNullable(o?.optString("video_url")),
                durationSec = if (o != null && o.has("duration_sec")) o.optDouble("duration_sec").toFloat() else null,
                loudnessLufs = if (o != null && o.has("loudness_lufs")) o.optDouble("loudness_lufs").toFloat() else null
            )
        }

        ReelV2JobStatus(
            reelJobId = j.optInt("reel_job_id", reelJobId),
            status = j.optString("status", "unknown"),
            currentStage = j.optString("current_stage").ifBlank { null },
            overallProgress = j.optInt("overall_progress", 0),
            errorCode = j.optString("error_code").ifBlank { null },
            errorMessage = j.optString("error_message").ifBlank { null },
            outputs = outputs
        )
    }

    fun processVideo(file: File, jobType: String = "video_reel", mode: String = "standard"): VoiceJobAccepted {
        return submitJob(
            endpoint = "video/process/",
            file = file,
            fields = mapOf("job_type" to jobType, "mode" to mode)
        )
    }

    fun fastLibClean(file: File): VoiceJobAccepted {
        return submitJob(
            endpoint = "lab/clean/",
            file = file,
            fields = emptyMap()
        )
    }

    fun fastLibVideoProcess(file: File): VoiceJobAccepted {
        return submitJob(
            endpoint = "lab/video/process/",
            file = file,
            fields = emptyMap()
        )
    }

    fun getBackgrounds(): List<VoiceBackground> = runNetwork("getBackgrounds") {
        val endpoint = baseUrl.trimEnd('/') + "/backgrounds/"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw VoiceCleaningException(friendlyHttpError(code, body))
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
        list
    }

    fun getStatus(jobId: Int): VoiceJobStatus = runNetwork("getStatus") {
        val endpoint = baseUrl.trimEnd('/') + "/status/$jobId/"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        val code = conn.responseCode
        val stream: InputStream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw VoiceCleaningException(friendlyHttpError(code, body))
        val j = JSONObject(body)
        VoiceJobStatus(
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

    private fun submitJob(endpoint: String, file: File, fields: Map<String, String>): VoiceJobAccepted = runNetwork("submitJob($endpoint)") {
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
        if (code !in 200..299) throw VoiceCleaningException(friendlyHttpError(code, body))
        val j = JSONObject(body)
        VoiceJobAccepted(
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

/** Map server HTTP codes to user-facing messages. */
internal fun friendlyHttpError(code: Int, body: String? = null): String = when (code) {
    413 -> "File too large — max 50 MB for video, 10 MB for audio."
    429 -> "Too many requests. Please wait a minute and try again."
    502, 503, 504 -> "Server is busy — please try again in a moment."
    401, 403 -> "Not allowed. Please sign in again."
    in 500..599 -> "Server error ($code). Please try again shortly."
    else -> {
        val snippet = body?.trim().orEmpty().take(120)
        if (snippet.isNotEmpty()) "HTTP $code — $snippet" else "HTTP $code"
    }
}
