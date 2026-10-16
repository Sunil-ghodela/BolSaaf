package com.reelvoice.audio

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ReelVoice voice-auth client. Talks to the `/voice/auth/` endpoint family.
 *
 * One opaque Bearer token per session (rotated on each login / cleared on logout).
 * Tokens + hydrated user profile are stored by the caller (MainActivity) in
 * SharedPreferences — this class is pure transport.
 */
class AuthApi(
    private val baseUrl: String = "https://voice.tranquilwaters.in/voice/"
) {
    companion object {
        private const val TAG = "AuthApi"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
    }

    private val base = baseUrl.trimEnd('/')

    data class Entitlements(
        val voiceMinutesMonthly: Int?,   // null = unlimited
        val cleaningModes: List<String>,
        val videoProcessing: Boolean,
        val videoExportWatermark: Boolean,
        val videoMinutesMonthly: Int?,
        val maxAudioUploadMb: Int,
        val maxVideoUploadMb: Int,
        val reelPresets: List<String>,
        val priorityQueue: Boolean,
        val adFree: Boolean,
        val extractVoice: Boolean,
        val addBackground: Boolean
    ) {
        companion object {
            val FREE_DEFAULT = Entitlements(
                voiceMinutesMonthly = 20,
                cleaningModes = listOf("basic", "standard"),
                videoProcessing = true,
                videoExportWatermark = true,
                videoMinutesMonthly = 10,
                maxAudioUploadMb = 25,
                maxVideoUploadMb = 50,
                reelPresets = listOf("Podcast", "Rain"),
                priorityQueue = false,
                adFree = false,
                extractVoice = true,
                addBackground = true
            )

            private fun intOrNull(o: JSONObject, key: String): Int? =
                if (o.has(key) && !o.isNull(key)) o.optInt(key) else null

            private fun stringList(o: JSONObject, key: String, fallback: List<String>): List<String> {
                val arr = o.optJSONArray(key) ?: return fallback
                return buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }
            }

            fun fromJson(o: JSONObject?): Entitlements {
                if (o == null) return FREE_DEFAULT
                return Entitlements(
                    // A present-but-null value means unlimited; a missing key means "unset".
                    voiceMinutesMonthly = if (o.has("voice_minutes_monthly")) intOrNull(o, "voice_minutes_monthly") else FREE_DEFAULT.voiceMinutesMonthly,
                    cleaningModes = stringList(o, "cleaning_modes", FREE_DEFAULT.cleaningModes),
                    videoProcessing = o.optBoolean("video_processing", true),
                    videoExportWatermark = o.optBoolean("video_export_watermark", true),
                    videoMinutesMonthly = if (o.has("video_minutes_monthly")) intOrNull(o, "video_minutes_monthly") else FREE_DEFAULT.videoMinutesMonthly,
                    maxAudioUploadMb = o.optInt("max_audio_upload_mb", FREE_DEFAULT.maxAudioUploadMb),
                    maxVideoUploadMb = o.optInt("max_video_upload_mb", FREE_DEFAULT.maxVideoUploadMb),
                    reelPresets = stringList(o, "reel_presets", FREE_DEFAULT.reelPresets),
                    priorityQueue = o.optBoolean("priority_queue", false),
                    adFree = o.optBoolean("ad_free", false),
                    extractVoice = o.optBoolean("extract_voice", true),
                    addBackground = o.optBoolean("add_background", true)
                )
            }
        }
    }

    data class AuthUser(
        val id: Int,
        val email: String,
        val displayName: String,
        val handle: String,
        val isPro: Boolean,
        val plan: String?,
        val proExpiresAt: String?,
        val autoRenewing: Boolean,
        val freeQuotaMinutes: Int,
        val freeMinutesLeft: Int,
        val freePeriodYyyyMm: String?,
        val entitlements: Entitlements
    ) {
        companion object {
            fun fromJson(o: JSONObject): AuthUser = AuthUser(
                id = o.optInt("id"),
                email = o.optString("email"),
                displayName = o.optString("display_name"),
                handle = o.optString("handle"),
                isPro = o.optBoolean("is_pro", false),
                plan = o.optString("plan").ifEmpty { null },
                proExpiresAt = o.optString("pro_expires_at").ifEmpty { null },
                autoRenewing = o.optBoolean("auto_renewing", false),
                freeQuotaMinutes = o.optInt("free_quota_minutes", 20),
                freeMinutesLeft = o.optInt("free_minutes_left", 20),
                freePeriodYyyyMm = o.optString("free_period_yyyymm").ifEmpty { null },
                entitlements = Entitlements.fromJson(o.optJSONObject("entitlements"))
            )
        }
    }

    /** Result of a server-side Play purchase validation. Pro is only granted when [isPro] is true. */
    data class BillingValidation(
        val ok: Boolean,
        val status: String,
        val reason: String?,
        val isPro: Boolean,
        val proExpiresAt: String?,
        val autoRenewing: Boolean
    )

    data class AuthResult(val token: String, val user: AuthUser)

    class AuthException(message: String, val status: Int) : Exception(message)

    /** POST /voice/auth/register/ → new user + session token. 409 if email taken. */
    fun register(email: String, password: String, displayName: String? = null): AuthResult {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
            if (!displayName.isNullOrBlank()) put("display_name", displayName)
        }
        val json = postJson("$base/auth/register/", body, token = null)
        return AuthResult(
            token = json.optString("token"),
            user = AuthUser.fromJson(json.getJSONObject("user"))
        )
    }

    /** POST /voice/auth/login/ → fresh session token (server rotates on every login). */
    fun login(email: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val json = postJson("$base/auth/login/", body, token = null)
        return AuthResult(
            token = json.optString("token"),
            user = AuthUser.fromJson(json.getJSONObject("user"))
        )
    }

    /** POST /voice/auth/logout/ — idempotent, always succeeds. */
    fun logout(token: String) {
        runCatching { postJson("$base/auth/logout/", JSONObject(), token = token) }
    }

    /** GET /voice/auth/me/ → current user (use to re-hydrate quota + pro status). */
    fun me(token: String): AuthUser {
        val json = getJson("$base/auth/me/", token)
        return AuthUser.fromJson(json.getJSONObject("user"))
    }

    /**
     * POST /voice/auth/consume/ {seconds} → server-side quota decrement.
     * Returns the refreshed user. Pro users never consume quota.
     */
    fun consumeQuota(token: String, seconds: Float): AuthUser {
        val body = JSONObject().put("seconds", seconds)
        val json = postJson("$base/auth/consume/", body, token = token)
        return AuthUser.fromJson(json.getJSONObject("user"))
    }

    /** POST /voice/auth/password-reset/ — always 200 (avoids email enumeration). */
    fun requestPasswordReset(email: String) {
        val body = JSONObject().put("email", email)
        postJson("$base/auth/password-reset/", body, token = null)
    }

    /**
     * POST /voice/billing/validate/ — server verifies a Play purchase token and grants Pro.
     * The server is the authority; the client never flips Pro locally. Returns a typed result so
     * the caller can surface invalid/upstream states without exceptions.
     */
    fun validatePurchase(authToken: String, productId: String, purchaseToken: String): BillingValidation {
        val body = JSONObject().apply {
            put("product_id", productId)
            put("purchase_token", purchaseToken)
        }
        val (code, json) = postJsonWithCode("$base/billing/validate/", body, token = authToken)
        val status = json.optString("status").ifEmpty { if (code in 200..299) "ok" else "error" }
        return BillingValidation(
            ok = code in 200..299 && status in setOf("ok", "already_active"),
            status = status,
            reason = json.optString("reason").ifEmpty { null },
            isPro = json.optBoolean("is_pro", false),
            proExpiresAt = json.optString("pro_expires_at").ifEmpty { null },
            autoRenewing = json.optBoolean("auto_renewing", false)
        )
    }

    // --- transport ---

    private fun postJson(endpoint: String, body: JSONObject, token: String?): JSONObject {
        val (code, json) = postJsonWithCode(endpoint, body, token)
        if (code !in 200..299) {
            val message = json.optString("error").ifEmpty { friendlyFallback(code) }
            Log.w(TAG, "auth error $code: $message")
            throw AuthException(message.ifBlank { friendlyFallback(code) }, code)
        }
        return json
    }

    /** Like [postJson] but never throws — returns the HTTP code and the parsed body (possibly {}). */
    private fun postJsonWithCode(endpoint: String, body: JSONObject, token: String?): Pair<Int, JSONObject> {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (raw.isBlank()) JSONObject()
                else runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
            return code to json
        } finally {
            conn.disconnect()
        }
    }

    private fun getJson(endpoint: String, token: String?): JSONObject {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            return readJsonOrThrow(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun readJsonOrThrow(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(raw).optString("error") }.getOrNull()
                ?: friendlyFallback(code)
            Log.w(TAG, "auth error $code: $message")
            throw AuthException(message.ifBlank { friendlyFallback(code) }, code)
        }
        return if (raw.isBlank()) JSONObject() else JSONObject(raw)
    }

    private fun friendlyFallback(code: Int): String = when (code) {
        400 -> "Please check what you entered and try again."
        401 -> "Invalid credentials."
        403 -> "Not allowed."
        409 -> "An account with that email already exists."
        429 -> "Too many attempts. Please wait a minute."
        in 500..599 -> "Server is busy — please try again in a moment."
        else -> "Unexpected error ($code)."
    }
}
