package com.bolsaaf.audio

import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class CloudCleanResult(
    val cleanedFile: File,
    val mode: String,
    val durationSec: Float?,
    val processingTimeSec: Float?,
    val message: String?
)

class VoiceCleaningApi(
    private val baseUrl: String = "https://shadowselfwork.com/voice/"
) {
    private val connectTimeoutMs = 20_000
    private val readTimeoutMs = 60_000

    fun cleanAudio(inputWav: File, outputWav: File, mode: String): CloudCleanResult {
        val cleanUrl = URL(baseUrl.trimEnd('/') + "/clean/")
        val boundary = "----BolSaafBoundary${UUID.randomUUID()}"
        val conn = (cleanUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }

        DataOutputStream(conn.outputStream).use { out ->
            writeFilePart(out, boundary, "file", inputWav, "audio/wav")
            writeTextPart(out, boundary, "mode", mode)
            out.writeBytes("--$boundary--\r\n")
            out.flush()
        }

        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()
            ?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("Cloud clean failed ($code): ${extractError(body)}")
        }

        val json = JSONObject(body)
        if (json.optString("status") != "success") {
            throw IllegalStateException(json.optString("message", "Cloud clean failed"))
        }
        val cleanedUrl = json.optString("cleaned_url")
        if (cleanedUrl.isBlank()) {
            throw IllegalStateException("Cloud clean response missing cleaned_url")
        }
        download(cleanedUrl, outputWav)

        return CloudCleanResult(
            cleanedFile = outputWav,
            mode = json.optString("mode", mode),
            durationSec = json.optDouble("duration", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
            processingTimeSec = json.optDouble("processing_time", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
            message = json.optString("message", null)
        )
    }

    private fun download(url: String, target: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            throw IllegalStateException("Download failed ($code)")
        }
        conn.inputStream.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun writeTextPart(out: DataOutputStream, boundary: String, name: String, value: String) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        out.writeBytes(value)
        out.writeBytes("\r\n")
    }

    private fun writeFilePart(
        out: DataOutputStream,
        boundary: String,
        fieldName: String,
        file: File,
        contentType: String
    ) {
        out.writeBytes("--$boundary\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${file.name}\"\r\n")
        out.writeBytes("Content-Type: $contentType\r\n\r\n")
        file.inputStream().use { input -> input.copyTo(out) }
        out.writeBytes("\r\n")
    }

    private fun extractError(body: String): String {
        return try {
            val json = JSONObject(body)
            json.optString("message", json.optString("error", body))
        } catch (_: Exception) {
            body.ifBlank { "Unknown error" }
        }
    }
}
