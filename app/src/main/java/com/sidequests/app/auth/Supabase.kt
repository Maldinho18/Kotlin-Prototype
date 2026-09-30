package com.sidequests.app.auth

import android.content.Context
import com.sidequests.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val email: String?,
)

class SupabaseAuthService(private val context: Context) {
    private val prefs = context.getSharedPreferences("supabase_auth", Context.MODE_PRIVATE)

    fun currentSession(): AuthSession? {
        val token = prefs.getString("access_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        return AuthSession(token, prefs.getString("refresh_token", null), userId, prefs.getString("email", null))
    }

    fun signOut() {
        prefs.edit().clear().apply()
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> = authenticate(email, password, signUp = false)

    suspend fun signUp(email: String, password: String): Result<AuthSession> = authenticate(email, password, signUp = true)

    private suspend fun authenticate(email: String, password: String, signUp: Boolean): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            require(BuildConfig.SUPABASE_URL.isNotBlank()) { "SUPABASE_URL is not configured." }
            require(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) { "SUPABASE_PUBLISHABLE_KEY is not configured." }
            require(email.contains("@")) { "Enter a valid email." }
            require(password.length >= 6) { "Password must have at least 6 characters." }

            val endpoint = if (signUp) {
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/signup"
            } else {
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/token?grant_type=password"
            }
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }.toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            val responseCode = connection.responseCode
            val response = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (responseCode !in 200..299) {
                val message = runCatching { JSONObject(response).optString("msg").ifBlank { JSONObject(response).optString("message") } }.getOrNull()
                error(message?.ifBlank { "Authentication failed ($responseCode)." } ?: "Authentication failed ($responseCode).")
            }

            val json = JSONObject(response)
            val accessToken = json.optString("access_token")
            val user = json.optJSONObject("user")
            val userId = user?.optString("id").orEmpty()
            if (accessToken.isBlank() || userId.isBlank()) {
                if (signUp) error("Account created. Check your email to confirm it, then sign in.")
                error("Authentication response did not contain a valid session.")
            }
            val session = AuthSession(
                accessToken = accessToken,
                refreshToken = json.optString("refresh_token").ifBlank { null },
                userId = userId,
                email = user?.optString("email")?.ifBlank { email.trim() },
            )
            prefs.edit()
                .putString("access_token", session.accessToken)
                .putString("refresh_token", session.refreshToken)
                .putString("user_id", session.userId)
                .putString("email", session.email)
                .apply()
            session
        }
    }
}
