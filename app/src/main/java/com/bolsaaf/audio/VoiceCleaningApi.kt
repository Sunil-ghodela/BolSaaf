package com.bolsaaf.audio

import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

data class CleanAudioResult(
    val outputFile: File,
    val processingTimeSec: Float? = null
)

data class VoiceApiHealth(
    val availableModes: Set<String>,
    val ffmpegAvailable: Boolean,
    val deepFilterNetAvailable: Boolean,
    val cudaAvailable: Boolean
)

/**
 * BolSaaf Voice Cleaning API (v2) client.
 *
 * - POST /voice/clean/ with multipart file + optional mode (basic | standard | studio | pro).
 * - Success may return relative [cleaned_url] (e.g. /media/cleaned/...) — resolved against API host.
 * - If [cleaned_url] is empty but [job_id] is present, polls GET /voice/status/{job_id}/ until completed or failed.
 */
class VoiceCleaningApi(
    private val baseUrl: String = "https://shadowselfwork.com/voice/"
) {

    companion object {
        private const val TAG = "VoiceCleaningApi"
        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val POLL_INTERVAL_MS = 1_500L
        private const val POLL_MAX_ATTEMPTS = 80
    }

    private val cleanEndpoint: String = baseUrl.trimEnd('/') + "/clean/"
    private val healthEndpoint: String = baseUrl.trimEnd('/') + "/health/"
    private val siteOrigin: String = run {
        val u = URL(baseUrl.trimEnd('/') + "/")
        val port = when {
            u.port == -1 -> ""
            u.port == u.defaultPort -> ""
            else -> ":${u.port}"
        }
        "${u.protocol}://${u.host}$port"
    }

    /**
     * Uploads [audioFile] and downloads the cleaned WAV to [outputFile].
     * @param mode API processing mode: basic, standard, studio, pro (server default is standard).
     */
    fun cleanAudio(
        audioFile: File,
        outputFile: File,
        mode: String = "standard"
    ): CleanAudioResult {
        Log.d(TAG, "cleanAudio start mode=$mode file=${audioFile.name} bytes=${audioFile.length()}")
        require(audioFile.exists()) { "Audio file does not exist: ${audioFile.absolutePath}" }
        val size = audioFile.length()
        require(size <= MAX_FILE_SIZE_BYTES) {
            "File size ${size / 1024}KB exceeds 5MB limit"
        }

        val boundary = "----WebKitFormBoundary${UUID.randomUUID()}"
        val connection = URL(cleanEndpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        connection.outputStream.use { out ->
            val fileName = audioFile.name.ifBlank { "recording.wav" }
            val mime = guessMimeType(fileName)
            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write(
                "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n".toByteArray(Charsets.UTF_8)
            )
            out.write("Content-Type: $mime\r\n\r\n".toByteArray(Charsets.UTF_8))
            audioFile.inputStream().use { it.copyTo(out) }
            out.write("\r\n".toByteArray(Charsets.UTF_8))
            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write("Content-Disposition: form-data; name=\"mode\"\r\n\r\n".toByteArray(Charsets.UTF_8))
            out.write(mode.toByteArray(Charsets.UTF_8))
            out.write("\r\n".toByteArray(Charsets.UTF_8))
            out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        val bodyStream: InputStream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        val responseText = bodyStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Log.d(TAG, "cleanAudio response code=$code body=${responseText.take(280)}")

        if (code !in 200..299) {
            Log.e(TAG, "API error $code: $responseText")
            throw VoiceCleaningException("Server returned $code: $responseText")
        }

        val json = try {
            JSONObject(responseText)
        } catch (e: Exception) {
            throw VoiceCleaningException("Invalid JSON response: $responseText", e)
        }

        if (json.optString("status") != "success") {
            val msg = json.optString("message", "Unknown error")
            throw VoiceCleaningException(msg)
        }

        val processingTimeSec = json.optDouble("processing_time", Double.NaN).let {
            if (it.isNaN()) null else it.toFloat()
        }

        var cleanedPath = json.optString("cleaned_url").trim()
        if (cleanedPath.isEmpty()) {
            val jobId = json.optInt("job_id", -1)
            if (jobId > 0) {
                Log.d(TAG, "cleaned_url missing, polling status for job_id=$jobId")
                cleanedPath = pollJobUntilUrl(jobId)
            }
        }
        if (cleanedPath.isEmpty()) {
            throw VoiceCleaningException("No cleaned_url in response and polling did not return a URL")
        }

        val downloadUrl = resolveMediaUrl(cleanedPath)
        Log.d(TAG, "Downloading cleaned audio from: $downloadUrl")
        try {
            downloadFile(downloadUrl, outputFile)
        } catch (e: VoiceCleaningException) {
            val msg = e.message.orEmpty()
            val is404 = msg.contains("404")
            val pk = if (is404) extractAudioPkFromCleanedPath(cleanedPath) else null
            if (!is404 || pk == null) throw e
            val underVoice = baseUrl.trimEnd('/') + "/cleaned_output/$pk/"
            val fallback = resolveMediaUrl(underVoice)
            Log.w(TAG, "/media download failed ($msg) — retry via Django route: $fallback")
            downloadFile(fallback, outputFile)
        }
        Log.d(TAG, "cleanAudio complete saved=${outputFile.absolutePath} processingTimeSec=$processingTimeSec")
        return CleanAudioResult(outputFile, processingTimeSec)
    }

    /** Filenames like `clean_33_original_....wav` embed the AudioFile pk. */
    private fun extractAudioPkFromCleanedPath(pathOrUrl: String): Int? {
        val seg = pathOrUrl.trim().substringAfterLast('/').substringBefore('?')
        val m = Regex("""clean_(\d+)_""").find(seg) ?: return null
        return m.groupValues[1].toIntOrNull()
    }

    fun getHealth(): VoiceApiHealth {
        val conn = URL(healthEndpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) {
            throw VoiceCleaningException("Health check failed: HTTP $code $body")
        }

        val j = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw VoiceCleaningException("Invalid health JSON: $body", e)
        }
        val available = LinkedHashSet<String>()
        val arr = j.optJSONArray("available_modes")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val mode = arr.optString(i).trim().lowercase(Locale.ROOT)
                if (mode.isNotEmpty()) available.add(mode)
            }
        }
        val req = j.optJSONObject("system_requirements")
        return VoiceApiHealth(
            availableModes = available,
            ffmpegAvailable = req?.optBoolean("ffmpeg", false) ?: false,
            deepFilterNetAvailable = req?.optBoolean("deepfilternet", false) ?: false,
            cudaAvailable = req?.optBoolean("cuda", false) ?: false
        ).also {
            Log.d(
                TAG,
                "health modes=${it.availableModes} ffmpeg=${it.ffmpegAvailable} deepfilter=${it.deepFilterNetAvailable} cuda=${it.cudaAvailable}"
            )
        }
    }

    private fun pollJobUntilUrl(jobId: Int): String {
        val statusUrl = baseUrl.trimEnd('/') + "/status/$jobId/"
        repeat(POLL_MAX_ATTEMPTS) { attempt ->
            Thread.sleep(if (attempt == 0) 0L else POLL_INTERVAL_MS)
            val conn = URL(statusUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code == 404) {
                throw VoiceCleaningException("Job not found: $jobId")
            }
            if (code !in 200..299) {
                Log.w(TAG, "Status poll $code: $text")
                return@repeat
            }
            val j = try {
                JSONObject(text)
            } catch (_: Exception) {
                Log.w(TAG, "Invalid status JSON: $text")
                return@repeat
            }
            val jobState = j.optString("status").ifEmpty { j.optString("state") }
            when (jobState) {
                "completed" -> {
                    val url = j.optString("cleaned_url").trim()
                    Log.d(TAG, "poll status=completed job_id=$jobId urlPresent=${url.isNotEmpty()}")
                    if (url.isNotEmpty()) return url
                }
                "failed" -> {
                    val err = j.optString("error_message").ifBlank { "Job failed" }
                    throw VoiceCleaningException(err)
                }
                "processing", "pending" -> { /* continue */ }
                else -> { /* unknown state — keep polling */ }
            }
        }
        throw VoiceCleaningException("Timed out waiting for job $jobId")
    }

    /**
     * Match [VoiceApiPhase2Client.resolveUrlNullable] and API docs: relative paths may omit the
     * leading slash (e.g. `media/cleaned/...`). Passing those raw to [URL] breaks or hits the
     * wrong host and yields Next.js 404 HTML.
     */
    private fun resolveMediaUrl(pathOrUrl: String): String {
        val p = pathOrUrl.trim()
        if (p.isEmpty() || p.equals("null", ignoreCase = true)) {
            throw VoiceCleaningException("cleaned_url is empty")
        }
        if (p.startsWith("http://", ignoreCase = true) || p.startsWith("https://", ignoreCase = true)) {
            return p
        }
        if (p.startsWith("//")) {
            return "https:$p"
        }
        if (p.startsWith("/")) {
            return siteOrigin + p
        }
        return "$siteOrigin/$p"
    }

    private fun shortenHttpErrorBody(body: String): String {
        val t = body.trim()
        if (t.isEmpty()) return ""
        val snippet = if (t.length > 220) t.take(220) + "…" else t
        return if (snippet.contains("<!DOCTYPE", ignoreCase = true) ||
            snippet.contains("<html", ignoreCase = true)
        ) {
            "(HTML error page — check cleaned_url / server routing)"
        } else {
            snippet
        }
    }

    private fun guessMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".m4a") || lower.endsWith(".aac") -> "audio/mp4"
            lower.endsWith(".ogg") -> "audio/ogg"
            lower.endsWith(".flac") -> "audio/flac"
            else -> "application/octet-stream"
        }
    }

    private fun downloadFile(urlString: String, outputFile: File) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        val code = connection.responseCode
        if (code !in 200..299) {
            val raw = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            val err = shortenHttpErrorBody(raw)
            throw VoiceCleaningException("Download failed: HTTP $code $err")
        }

        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}

class VoiceCleaningException(message: String, cause: Throwable? = null) : Exception(message, cause)
