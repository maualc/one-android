package com.one.cognitivecompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
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
    val expiresAt: Instant?,
    val backendRole: String = role.wireValue
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

data class FamilyInviteAcceptRequest(
    val code: String,
    val displayName: String?
)

data class FamilyInviteRequest(
    val displayName: String,
    val email: String? = null,
    val role: OneRole = OneRole.CAREGIVER,
    val expiresInSeconds: Long = 86_400
)

data class OneFamilyInvite(
    val id: UUID,
    val code: String,
    val role: String,
    val expiresInSeconds: Long,
    val syntheticDemo: Boolean
)

data class ConsentRequest(
    val purpose: String,
    val policyVersion: String,
    val granted: Boolean,
    val subjectUserId: UUID? = null
)

data class OneRemoteConsent(
    val id: UUID,
    val subjectUserId: UUID,
    val purpose: String,
    val policyVersion: String,
    val grantedAt: Instant?,
    val revokedAt: Instant?
)

data class OneDataExport(
    val homeId: UUID,
    val exportedAt: Instant?,
    val recordCounts: Map<String, Int>
)

data class OneDataDeletion(
    val requestId: UUID?,
    val status: String
)

data class BackendHealth(
    val status: String,
    val database: String?,
    val databaseStatus: String?,
    val localInferenceModel: String?
)

data class OneCheckInResult(
    val id: UUID?,
    val status: String,
    val trend: String,
    val explanation: String,
    val evidenceIds: List<String>,
    val limitations: String,
    val degraded: Boolean
)

data class OneLiveKitToken(
    val serverUrl: String,
    val participantToken: String,
    val expiresInSeconds: Long,
    val mode: String
)

data class OneRemoteEventSignal(
    val eventName: String,
    val eventId: UUID?,
    val observedAt: Instant?
)

data class OneHomeProfile(
    val homeId: UUID,
    val homeName: String,
    val residentName: String,
    val paused: Boolean
)

data class OneRemoteCamera(
    val id: UUID,
    val name: String,
    val roomId: UUID?,
    val platform: String,
    val status: String,
    val enabled: Boolean,
    val lastSeenAt: Instant?
)

data class OneRemoteObject(
    val id: UUID,
    val label: String,
    val status: String,
    val zone: String?,
    val pointX: Double?,
    val pointY: Double?,
    val lastSeenAt: Instant?,
    val confidence: Double,
    val confidenceRadiusM: Double
)

data class OneRemoteEvent(
    val id: UUID,
    val type: String,
    val status: String,
    val explanation: String,
    val confidence: Double,
    val lastSeenAt: Instant?
)

data class OneRemoteClip(
    val id: UUID,
    val eventId: UUID,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val expiresAt: Instant?
)

data class OneRemoteFamilyMember(
    val id: UUID,
    val displayName: String,
    val email: String?,
    val role: String,
    val representationStatus: String?
)

data class OneRemoteMedicationReminder(
    val planId: UUID,
    val name: String,
    val dose: String,
    val instructions: String,
    val scheduleRule: String,
    val scheduledFor: Instant?,
    val status: String,
    val note: String?,
    val assignedCaregiverName: String?
)

class OneApiException(message: String, val statusCode: Int? = null, cause: Throwable? = null) : IOException(message, cause)

interface OneApiClient {
    suspend fun health(): BackendHealth
    suspend fun startPairing(pairingRequest: PairingStartRequest, bootstrapSecret: String? = null): PairingStartResponse
    suspend fun completePairing(code: String): OneSession
    suspend fun acceptFamilyInvite(inviteRequest: FamilyInviteAcceptRequest): OneSession
    suspend fun createFamilyInvite(session: OneSession, inviteRequest: FamilyInviteRequest): OneFamilyInvite
    suspend fun recordConsent(session: OneSession, consentRequest: ConsentRequest)
    suspend fun homeConsents(session: OneSession): List<OneRemoteConsent>
    suspend fun requestDataExport(session: OneSession): OneDataExport
    suspend fun requestDataDeletion(session: OneSession): OneDataDeletion
    suspend fun logout(session: OneSession)
    suspend fun liveKitToken(session: OneSession, mode: String = "subscribe"): OneLiveKitToken
    suspend fun streamHomeEvents(session: OneSession, onEvent: suspend (OneRemoteEventSignal) -> Unit)
    fun clipContentUrl(session: OneSession, clipId: UUID): String
    suspend fun homeProfile(session: OneSession): OneHomeProfile
    suspend fun homeCameras(session: OneSession): List<OneRemoteCamera>
    suspend fun homeObjects(session: OneSession): List<OneRemoteObject>
    suspend fun homeEvents(session: OneSession, limit: Int = 50): List<OneRemoteEvent>
    suspend fun homeClips(session: OneSession): List<OneRemoteClip>
    suspend fun familyMembers(session: OneSession): List<OneRemoteFamilyMember>
    suspend fun medicationReminders(session: OneSession, day: String? = null, subjectUserId: UUID? = null): List<OneRemoteMedicationReminder>
    suspend fun markMedicationCheckIn(
        session: OneSession,
        planId: UUID,
        scheduledFor: Instant,
        status: String,
        note: String = ""
    )
    suspend fun submitCheckIn(session: OneSession, transcript: String, subjectUserId: UUID? = null): OneCheckInResult
}

/**
 * Small dependency-free adapter for the versioned FastAPI contract. Keeping
 * this behind an interface lets the UI use a deterministic fake while the
 * backend is unavailable, matching the iOS MVP's demo behaviour.
 */
class OneHttpApiClient(
    private val configuration: RuntimeConfiguration = RuntimeConfiguration()
) : OneApiClient {
    override fun clipContentUrl(session: OneSession, clipId: UUID): String =
        configuration.apiBaseUrl.trimEnd('/') + "/clips/$clipId/content"

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
        return sessionFrom(body)
    }

    override suspend fun acceptFamilyInvite(inviteRequest: FamilyInviteAcceptRequest): OneSession {
        val payload = JSONObject().put("code", inviteRequest.code)
        inviteRequest.displayName?.let { payload.put("display_name", it) }
        return sessionFrom(request("/family/invites/accept", "POST", payload))
    }

    override suspend fun createFamilyInvite(session: OneSession, inviteRequest: FamilyInviteRequest): OneFamilyInvite {
        val payload = JSONObject()
            .put("display_name", inviteRequest.displayName)
            .put("role", inviteRequest.role.wireValue)
            .put("expires_in_seconds", inviteRequest.expiresInSeconds)
        inviteRequest.email?.let { payload.put("email", it) }
        val body = request(
            "/homes/${session.homeId}/family/invites",
            "POST",
            payload,
            token = session.accessToken
        )
        return OneFamilyInvite(
            id = body.requiredUuid("id"),
            code = body.requiredString("code"),
            role = body.optString("role").takeIf { it.isNotBlank() } ?: inviteRequest.role.wireValue,
            expiresInSeconds = body.optLong("expires_in_seconds", inviteRequest.expiresInSeconds),
            syntheticDemo = body.optBoolean("synthetic_demo", false)
        )
    }

    private suspend fun sessionFrom(body: JSONObject): OneSession {
        val accessToken = body.requiredString("access_token")
        val homeId = body.requiredUuid("home_id")
        val userId = body.requiredUuid("user_id")
        val expiresAt = body.optLong("expires_in", -1).takeIf { it >= 0 }?.let { Instant.now().plusSeconds(it) }
        val backendRole = runCatching {
            request("/me", "GET", token = accessToken).getJSONObject("actor").optString("role")
        }.getOrDefault("caregiver")
        val role = backendRole.toOneRole()
        return OneSession(accessToken, homeId, userId, role, expiresAt, backendRole)
    }

    override suspend fun recordConsent(session: OneSession, consentRequest: ConsentRequest) {
        val payload = JSONObject()
            .put("purpose", consentRequest.purpose)
            .put("policy_version", consentRequest.policyVersion)
            .put("granted", consentRequest.granted)
        consentRequest.subjectUserId?.let { payload.put("subject_user_id", it.toString()) }
        request("/homes/${session.homeId}/consents", "POST", payload, token = session.accessToken)
    }

    override suspend fun homeConsents(session: OneSession): List<OneRemoteConsent> {
        val rows = request("/homes/${session.homeId}/consents", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                val subjectUserId = runCatching { UUID.fromString(row.optString("subject_user_id")) }.getOrNull() ?: continue
                val purpose = row.optString("purpose").takeIf { it.isNotBlank() } ?: continue
                add(
                    OneRemoteConsent(
                        id = id,
                        subjectUserId = subjectUserId,
                        purpose = purpose,
                        policyVersion = row.optString("policy_version"),
                        grantedAt = row.optNullableString("granted_at")?.toInstantOrNull(),
                        revokedAt = row.optNullableString("revoked_at")?.toInstantOrNull()
                    )
                )
            }
        }
    }

    override suspend fun requestDataExport(session: OneSession): OneDataExport {
        val body = request("/homes/${session.homeId}/privacy/export", "POST", token = session.accessToken)
        val data = body.optJSONObject("data")
        val recordCounts = if (data == null) {
            emptyMap()
        } else {
            buildMap {
                val keys = data.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val rows = data.optJSONArray(key)
                    if (rows != null) put(key, rows.length())
                }
            }
        }
        return OneDataExport(
            homeId = body.optNullableUuid("home_id") ?: session.homeId,
            exportedAt = body.optNullableString("exported_at")?.toInstantOrNull(),
            recordCounts = recordCounts
        )
    }

    override suspend fun requestDataDeletion(session: OneSession): OneDataDeletion {
        val body = request("/homes/${session.homeId}/privacy/delete", "POST", token = session.accessToken)
        return OneDataDeletion(
            requestId = body.optNullableUuid("request_id"),
            status = body.optString("status").takeIf { it.isNotBlank() } ?: "unknown"
        )
    }

    override suspend fun logout(session: OneSession) {
        request("/sessions/current", "DELETE", token = session.accessToken)
    }

    override suspend fun liveKitToken(session: OneSession, mode: String): OneLiveKitToken {
        val requestedMode = mode.lowercase().takeIf { it in setOf("auto", "publish", "subscribe") }
            ?: throw OneApiException("Invalid LiveKit mode.")
        val body = request(
            "/homes/${session.homeId}/livekit/token",
            "POST",
            JSONObject().put("mode", requestedMode),
            token = session.accessToken
        )
        return OneLiveKitToken(
            serverUrl = body.requiredString("url"),
            participantToken = body.requiredString("token"),
            expiresInSeconds = body.requiredLong("expires_in"),
            mode = body.optString("mode").takeIf { it.isNotBlank() } ?: requestedMode
        )
    }

    override suspend fun streamHomeEvents(session: OneSession, onEvent: suspend (OneRemoteEventSignal) -> Unit) = withContext(Dispatchers.IO) {
        val connection = (URL(configuration.apiBaseUrl.trimEnd('/') + "/homes/${session.homeId}/events/stream").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
            doInput = true
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                val raw = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw OneApiException("ONE API error ($status): ${raw.problemMessage()}", status)
            }
            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)
            var eventName: String? = null
            val data = StringBuilder()
            while (true) {
                currentCoroutineContext().ensureActive()
                val line = reader.readLine() ?: break
                when {
                    line.startsWith(": connected") -> onEvent(OneRemoteEventSignal("one.connected.v1", null, null))
                    line.startsWith(":") -> Unit
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim().takeIf { it.isNotBlank() }
                    line.startsWith("data:") -> {
                        if (data.length > 1_000_000) throw OneApiException("ONE event payload is too large.")
                        if (data.isNotEmpty()) data.append('\n')
                        data.append(line.removePrefix("data:").trim())
                    }
                    line.isBlank() -> {
                        val completedEvent = eventName
                        val completedData = data.toString()
                        if (completedEvent != null && completedData.isNotBlank()) {
                            val payload = runCatching { JSONObject(completedData) }.getOrNull()
                            if (payload != null) {
                                onEvent(
                                    OneRemoteEventSignal(
                                        eventName = completedEvent,
                                        eventId = payload.optNullableUuid("event_id"),
                                        observedAt = payload.optNullableString("observed_at").toInstantOrNull()
                                    )
                                )
                            }
                        }
                        eventName = null
                        data.clear()
                    }
                }
            }
        } catch (error: SocketTimeoutException) {
            throw OneApiException("The ONE event stream timed out.", cause = error)
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun homeProfile(session: OneSession): OneHomeProfile {
        val body = request("/me", "GET", token = session.accessToken)
        val home = body.optJSONObject("home") ?: throw OneApiException("ONE API response is missing the home profile.")
        return OneHomeProfile(
            homeId = home.requiredUuid("id"),
            homeName = home.requiredString("name"),
            residentName = home.optString("residentName").takeIf { it.isNotBlank() } ?: "Resident",
            paused = body.optBoolean("paused", false)
        )
    }

    override suspend fun homeCameras(session: OneSession): List<OneRemoteCamera> {
        val rows = request("/homes/${session.homeId}/cameras", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                add(
                    OneRemoteCamera(
                        id = id,
                        name = row.optString("name").takeIf { it.isNotBlank() }
                            ?: row.optString("label").takeIf { it.isNotBlank() }
                            ?: "Unnamed camera",
                        roomId = row.optNullableUuid("room_id") ?: row.optNullableUuid("roomId"),
                        platform = row.optString("platform").takeIf { it.isNotBlank() } ?: "unknown",
                        status = row.optString("status").takeIf { it.isNotBlank() } ?: "unknown",
                        enabled = row.optNullableBoolean("enabled") ?: true,
                        lastSeenAt = (row.optNullableString("lastSeenAt") ?: row.optNullableString("last_seen_at")).toInstantOrNull()
                    )
                )
            }
        }
    }

    override suspend fun homeObjects(session: OneSession): List<OneRemoteObject> {
        val rows = request("/homes/${session.homeId}/objects", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                add(
                    OneRemoteObject(
                        id = id,
                        label = row.optString("label").takeIf { it.isNotBlank() } ?: "Unlabelled object",
                        status = row.optString("status").takeIf { it.isNotBlank() } ?: "unknown",
                        zone = row.optNullableString("zone"),
                        pointX = row.optJSONObject("point")?.optNullableDouble("x"),
                        pointY = row.optJSONObject("point")?.optNullableDouble("y"),
                        lastSeenAt = (row.optNullableString("lastSeenAt") ?: row.optNullableString("last_seen_at")).toInstantOrNull(),
                        confidence = row.optDouble("confidence", 0.0).takeUnless { it.isNaN() } ?: 0.0,
                        confidenceRadiusM = row.optDouble("confidenceRadiusM", 0.0).takeUnless { it.isNaN() } ?: 0.0
                    )
                )
            }
        }
    }

    override suspend fun homeEvents(session: OneSession, limit: Int): List<OneRemoteEvent> {
        val boundedLimit = limit.coerceIn(1, 100)
        val rows = request("/homes/${session.homeId}/events?limit=$boundedLimit", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                add(
                    OneRemoteEvent(
                        id = id,
                        type = row.optString("event_type").takeIf { it.isNotBlank() } ?: "unknown",
                        status = row.optString("status").takeIf { it.isNotBlank() } ?: "unknown",
                        explanation = row.optString("explanation").takeIf { it.isNotBlank() } ?: "No explanation provided.",
                        confidence = row.optDouble("confidence", 0.0).takeUnless { it.isNaN() } ?: 0.0,
                        lastSeenAt = (row.optNullableString("last_seen_at") ?: row.optNullableString("lastSeenAt")).toInstantOrNull()
                    )
                )
            }
        }
    }

    override suspend fun homeClips(session: OneSession): List<OneRemoteClip> {
        val rows = request("/homes/${session.homeId}/clips", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                val eventId = runCatching { UUID.fromString(row.optString("event_id")) }.getOrNull() ?: continue
                add(
                    OneRemoteClip(
                        id = id,
                        eventId = eventId,
                        startsAt = row.optNullableString("starts_at")?.toInstantOrNull(),
                        endsAt = row.optNullableString("ends_at")?.toInstantOrNull(),
                        expiresAt = row.optNullableString("expires_at")?.toInstantOrNull()
                    )
                )
            }
        }
    }

    override suspend fun familyMembers(session: OneSession): List<OneRemoteFamilyMember> {
        val rows = request("/homes/${session.homeId}/family/members", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                val displayName = row.optString("display_name").takeIf { it.isNotBlank() } ?: continue
                add(
                    OneRemoteFamilyMember(
                        id = id,
                        displayName = displayName,
                        email = row.optNullableString("email"),
                        role = row.optString("role").takeIf { it.isNotBlank() } ?: "member",
                        representationStatus = row.optNullableString("representation_status")
                    )
                )
            }
        }
    }

    override suspend fun medicationReminders(session: OneSession, day: String?, subjectUserId: UUID?): List<OneRemoteMedicationReminder> {
        val query = buildList {
            day?.let { add("day=$it") }
            subjectUserId?.let { add("subject_user_id=$it") }
        }.joinToString("&").takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
        val rows = request("/homes/${session.homeId}/medication-reminders$query", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val planId = runCatching { UUID.fromString(row.optString("plan_id")) }.getOrNull() ?: continue
                val name = row.optString("name").takeIf { it.isNotBlank() } ?: continue
                add(
                    OneRemoteMedicationReminder(
                        planId = planId,
                        name = name,
                        dose = row.optString("dose"),
                        instructions = row.optString("instructions"),
                        scheduleRule = row.optString("schedule_rule"),
                        scheduledFor = row.optNullableString("scheduled_for").toInstantOrNull(),
                        status = row.optString("status").takeIf { it.isNotBlank() } ?: "pending",
                        note = row.optNullableString("note"),
                        assignedCaregiverName = row.optNullableString("assigned_caregiver_name")
                    )
                )
            }
        }
    }

    override suspend fun markMedicationCheckIn(
        session: OneSession,
        planId: UUID,
        scheduledFor: Instant,
        status: String,
        note: String
    ) {
        val normalizedStatus = status.lowercase()
        require(normalizedStatus in setOf("pending", "taken", "skipped", "missed")) {
            "Medication check-in status is not supported."
        }
        val payload = JSONObject()
            .put("scheduled_for", scheduledFor.toString())
            .put("status", normalizedStatus)
            .put("note", note.take(500))
        request(
            "/homes/${session.homeId}/medication-plans/$planId/check-ins",
            "POST",
            payload,
            token = session.accessToken
        )
    }

    override suspend fun submitCheckIn(session: OneSession, transcript: String, subjectUserId: UUID?): OneCheckInResult {
        val payload = JSONObject().put("transcript", transcript.take(4_000))
        subjectUserId?.let { payload.put("subject_user_id", it.toString()) }
        val body = request(
            "/homes/${session.homeId}/check-ins",
            "POST",
            payload,
            token = session.accessToken
        )
        val evidenceRows = body.optJSONArray("evidence_ids")
        val evidenceIds = if (evidenceRows == null) {
            emptyList()
        } else {
            buildList {
                for (index in 0 until evidenceRows.length()) {
                    evidenceRows.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        return OneCheckInResult(
            id = body.optNullableUuid("id"),
            status = body.optString("status").takeIf { it.isNotBlank() } ?: "unknown",
            trend = body.optString("trend").takeIf { it.isNotBlank() } ?: "unknown",
            explanation = body.optString("explanation").takeIf { it.isNotBlank() } ?: "No check-in summary was returned.",
            evidenceIds = evidenceIds,
            limitations = body.optString("limitations").takeIf { it.isNotBlank() } ?: "This is an administrative summary, not medical advice.",
            degraded = body.optBoolean("degraded", false)
        )
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

private fun JSONObject.optNullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

private fun JSONObject.optNullableUuid(key: String): UUID? = optNullableString(key)?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }

private fun JSONObject.optNullableBoolean(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> null
    }
}

private fun String?.toInstantOrNull(): Instant? = this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }

private fun String.problemMessage(): String = runCatching { JSONObject(this).optString("detail").takeIf { it.isNotBlank() } ?: JSONObject(this).optJSONObject("error")?.optString("message") }.getOrNull() ?: "Request failed."
