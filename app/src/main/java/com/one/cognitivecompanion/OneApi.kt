package com.one.cognitivecompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID

data class RuntimeConfiguration(
    val apiBaseUrl: String = BuildConfig.ONE_API_BASE_URL
) {
    val isDemoEndpoint: Boolean
        get() = apiBaseUrl.contains("10.0.2.2") || apiBaseUrl.contains("127.0.0.1")
}

data class OneSession(
    val accessToken: String,
    val homeId: UUID,
    val userId: UUID,
    val role: OneRole,
    val expiresAt: Instant?
)

data class PairingStartRequest(
    val displayName: String,
    val email: String?,
    val homeName: String,
    val role: OneRole = OneRole.CAREGIVER
)

data class PairingStartResponse(
    val pairingCode: String,
    val expiresInSeconds: Long,
    val homeId: UUID,
    val userId: UUID,
    val role: String
)

data class ConsentRequest(
    val purpose: String,
    val policyVersion: String,
    val granted: Boolean,
    val subjectUserId: UUID? = null
)

data class BackendHealth(
    val status: String,
    val database: String?,
    val databaseStatus: String?,
    val localInferenceModel: String?
)

class OneApiException(message: String, val statusCode: Int? = null, cause: Throwable? = null) : IOException(message, cause)

interface OneApiClient {
    suspend fun health(): BackendHealth
    suspend fun startPairing(pairingRequest: PairingStartRequest, bootstrapSecret: String? = null): PairingStartResponse
    suspend fun completePairing(code: String): OneSession
    suspend fun recordConsent(session: OneSession, consentRequest: ConsentRequest)
    suspend fun logout(session: OneSession)
}

/**
 * Small dependency-free adapter for the versioned FastAPI contract. Keeping
 * this behind an interface lets the UI use a deterministic fake while the
 * backend is unavailable, matching the iOS MVP's demo behaviour.
 */
class OneHttpApiClient(
    private val configuration: RuntimeConfiguration = RuntimeConfiguration()
) : OneApiClient {
    override suspend fun health(): BackendHealth {
        val body = request(path = "/health", method = "GET")
        return BackendHealth(
            status = body.optString("status", "unknown"),
            database = body.optNullableString("database"),
            databaseStatus = body.optNullableString("database_status"),
            localInferenceModel = body.optNullableString("local_inference_model")
        )
    }

    override suspend fun startPairing(pairingRequest: PairingStartRequest, bootstrapSecret: String?): PairingStartResponse {
        val payload = JSONObject()
            .put("display_name", pairingRequest.displayName)
            .put("home_name", pairingRequest.homeName)
            .put("role", pairingRequest.role.wireValue)
        pairingRequest.email?.let { payload.put("email", it) }
        val body = request("/pairing/start", "POST", payload, extraHeaders = bootstrapSecret?.let { mapOf("X-Bootstrap-Secret" to it) }.orEmpty())
        return PairingStartResponse(
            pairingCode = body.requiredString("pairing_code"),
            expiresInSeconds = body.requiredLong("expires_in_seconds"),
            homeId = body.requiredUuid("home_id"),
            userId = body.requiredUuid("user_id"),
            role = body.requiredString("role")
        )
    }

    override suspend fun completePairing(code: String): OneSession {
        val body = request("/pairing/complete", "POST", JSONObject().put("code", code))
        val accessToken = body.requiredString("access_token")
        val homeId = body.requiredUuid("home_id")
        val userId = body.requiredUuid("user_id")
        val expiresAt = body.optLong("expires_in", -1).takeIf { it >= 0 }?.let { Instant.now().plusSeconds(it) }
        val role = runCatching {
            request("/me", "GET", token = accessToken).getJSONObject("actor").optString("role")
        }.getOrDefault("caregiver").toOneRole()
        return OneSession(accessToken, homeId, userId, role, expiresAt)
    }

    override suspend fun recordConsent(session: OneSession, consentRequest: ConsentRequest) {
        val payload = JSONObject()
            .put("purpose", consentRequest.purpose)
            .put("policy_version", consentRequest.policyVersion)
            .put("granted", consentRequest.granted)
        consentRequest.subjectUserId?.let { payload.put("subject_user_id", it.toString()) }
        request("/homes/${session.homeId}/consents", "POST", payload, token = session.accessToken)
    }

    override suspend fun logout(session: OneSession) {
        request("/sessions/current", "DELETE", token = session.accessToken)
    }

    private suspend fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        token: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(configuration.apiBaseUrl.trimEnd('/') + "/" + path.trimStart('/')).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            body?.let { setRequestProperty("Content-Type", "application/json") }
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
        }

        try {
            body?.let {
                connection.doOutput = true
                connection.outputStream.use { output -> output.write(it.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw OneApiException("ONE API error ($status): ${raw.problemMessage()}", status)
            if (raw.isBlank()) JSONObject() else JSONObject(raw)
        } catch (error: OneApiException) {
            throw error
        } catch (error: Exception) {
            throw OneApiException("Could not reach the ONE API.", cause = error)
        } finally {
            connection.disconnect()
        }
    }
}

internal val OneRole.wireValue: String
    get() = if (this == OneRole.RESIDENT) "resident" else "caregiver"

internal fun String.toOneRole(): OneRole = if (equals("resident", ignoreCase = true)) OneRole.RESIDENT else OneRole.CAREGIVER

private fun JSONObject.requiredString(key: String): String = optString(key).takeIf { it.isNotBlank() } ?: throw OneApiException("ONE API response is missing '$key'.")

private fun JSONObject.requiredLong(key: String): Long = if (has(key) && !isNull(key)) optLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: throw OneApiException("ONE API response has an invalid '$key'.") else throw OneApiException("ONE API response is missing '$key'.")

private fun JSONObject.requiredUuid(key: String): UUID = runCatching { UUID.fromString(requiredString(key)) }.getOrElse { throw OneApiException("ONE API response has an invalid '$key'.", cause = it) }

private fun JSONObject.optNullableString(key: String): String? = optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun String.problemMessage(): String = runCatching { JSONObject(this).optString("detail").takeIf { it.isNotBlank() } ?: JSONObject(this).optJSONObject("error")?.optString("message") }.getOrNull() ?: "Request failed."
