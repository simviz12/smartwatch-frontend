package com.pulso.mobile.data.auth

import android.content.Context
import com.pulso.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class Session(val token: String, val userId: String, val name: String, val email: String)

/** Result of a sign-in / sign-up call; [errorCode] lets the UI show a localized message. */
sealed class AuthResult {
    data class Success(val session: Session) : AuthResult()
    data class Failure(val errorCode: String, val serverMessage: String?) : AuthResult()
}

/**
 * Talks to the Pulso backend (`/v1/auth/...`) and keeps the session on the device.
 * The token is stored in app-private preferences; signing out removes it.
 */
class AuthRepository private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val baseUrl: String get() = BuildConfig.PULSO_API_URL

    /** False when the APK was built without PULSO_API_URL (see the README). */
    val isServerConfigured: Boolean get() = baseUrl.startsWith("http")

    suspend fun login(email: String, password: String): AuthResult =
        authCall("/v1/auth/login", JSONObject().put("email", email.trim()).put("password", password))

    suspend fun register(name: String, email: String, password: String): AuthResult =
        authCall(
            "/v1/auth/register",
            JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password)
        )

    /** Re-validates a stored session; clears it if the server says the token is no longer valid. */
    suspend fun refreshProfile() {
        val current = _session.value ?: return
        if (!isServerConfigured) return
        withContext(Dispatchers.IO) {
            try {
                val conn = open("/v1/auth/me", "GET")
                conn.setRequestProperty("Authorization", "Bearer ${current.token}")
                if (conn.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    signOut()
                } else if (conn.responseCode in 200..299) {
                    val user = JSONObject(conn.inputStream.bufferedReader().readText())
                    save(current.copy(name = user.optString("name", current.name), email = user.optString("email", current.email)))
                }
                conn.disconnect()
            } catch (_: IOException) {
                // Offline or server asleep: keep the local session, the app works without the network.
            }
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private suspend fun authCall(path: String, body: JSONObject): AuthResult = withContext(Dispatchers.IO) {
        if (!isServerConfigured) return@withContext AuthResult.Failure(ERROR_NO_SERVER, null)
        try {
            val conn = open(path, "POST")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
            conn.disconnect()
            if (code in 200..299) {
                val json = JSONObject(text)
                val user = json.getJSONObject("user")
                val session = Session(
                    token = json.getString("token"),
                    userId = user.getString("id"),
                    name = user.optString("name"),
                    email = user.optString("email")
                )
                save(session)
                AuthResult.Success(session)
            } else {
                val error = runCatching { JSONObject(text).getJSONObject("error") }.getOrNull()
                val details = error?.optJSONArray("details")
                val message = details?.optString(0)?.takeIf { it.isNotBlank() } ?: error?.optString("message")
                AuthResult.Failure(error?.optString("code") ?: "HTTP_$code", message)
            }
        } catch (_: SocketTimeoutException) {
            AuthResult.Failure(ERROR_TIMEOUT, null)
        } catch (_: IOException) {
            AuthResult.Failure(ERROR_NETWORK, null)
        } catch (e: Exception) {
            AuthResult.Failure(ERROR_UNKNOWN, e.message)
        }
    }

    private fun open(path: String, method: String): HttpURLConnection =
        (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            // Free hosting sleeps when idle; the first request can take ~1 minute to wake it.
            connectTimeout = 70_000
            readTimeout = 70_000
        }

    private fun save(session: Session) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_NAME, session.name)
            .putString(KEY_EMAIL, session.email)
            .apply()
        _session.value = session
    }

    private fun loadSession(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Session(
            token = token,
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty()
        )
    }

    companion object {
        const val ERROR_NETWORK = "NETWORK"
        const val ERROR_TIMEOUT = "TIMEOUT"
        const val ERROR_UNKNOWN = "UNKNOWN"
        const val ERROR_NO_SERVER = "NO_SERVER"
        private const val PREFS = "pulso_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: AuthRepository(context).also { INSTANCE = it } }
    }
}
