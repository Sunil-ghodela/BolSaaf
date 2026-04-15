package com.bolsaaf.audio

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * BolSaaf voice-auth client (server: /voice/auth/*).
 *
 * One opaque Bearer token per session (rotated on each login / cleared on logout).
 * Tokens + hydrated user profile are stored by the caller (MainActivity) in
 * SharedPreferences — this class is pure transport.
 */
class AuthApi(
    private val baseUrl: String = "https://shadowselfwork.com/voice/"
) {
    companion object {
        private const val TAG = "AuthApi"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
    }

    private val base = baseUrl.trimEnd('/')

    data class AuthUser(
        val id: Int,
        val email: String,
        val displayName: String,
        val handle: String,
        val isPro: Boolean,
        val proExpiresAt: String?,
        val freeQuotaMinutes: Int,
        val freeMinutesLeft: Int,
        val freePeriodYyyyMm: String?
    ) {
        companion object {
            fun fromJson(o: JSONObject): AuthUser = AuthUser(
                id = o.optInt("id"),
                email = o.optString("email"),
                displayName = o.optString("display_name"),
                handle = o.optString("handle"),
                isPro = o.optBoolean("is_pro", false),
                proExpiresAt = o.optString("pro_expires_at").ifEmpty { null },
                freeQuotaMinutes = o.optInt("free_quota_minutes", 20),
                freeMinutesLeft = o.optInt("free_minutes_left", 20),
                freePeriodYyyyMm = o.optString("free_period_yyyymm").ifEmpty { null }
            )
        }
    }

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

    // --- transport ---

    private fun postJson(endpoint: String, body: JSONObject, token: String?): JSONObject {
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
            return readJsonOrThrow(conn)
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
