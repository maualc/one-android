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
    val backendRole: String = role.wireValue,
    /** Short-lived publisher credential used to recover after a process restart. */
    val reconnectToken: String? = null
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

/** Passwordless identity challenge used by the live account flow. */
data class EmailAuthRequest(
    val email: String,
    val purpose: String,
    val displayName: String? = null,
    val homeName: String = "ONE Home",
    val careSetting: String = "home",
    val supportFocus: String = "general",
    val role: OneRole = OneRole.CAREGIVER
)

data class EmailAuthVerifyRequest(
    val email: String,
    val code: String
)

data class EmailAuthChallenge(
    val verificationId: UUID,
    val expiresInSeconds: Long,
    val delivery: String,
    val devCode: String?,
    val email: String,
    val purpose: String,
    val homeId: UUID,
    val userId: UUID,
    val role: String
)

data class OneCareSpace(
    val id: UUID,
    val name: String,
    val careSetting: String,
    val supportFocus: String,
    val residentName: String,
    val recipientNames: List<String>,
    val recipientCount: Int,
    val role: String,
    val active: Boolean
)

data class CareSpaceCreateRequest(
    val name: String,
    val careSetting: String = "home",
    val supportFocus: String = "general"
)

data class OneCareRecipient(
    val id: UUID,
    val displayName: String,
    val relationship: String?,
    val roomLabel: String?,
    val createdAt: Instant?
)

data class CareRecipientCreateRequest(
    val displayName: String,
    val relationship: String? = null,
    val roomLabel: String? = null
)

data class CareRecipientUpdateRequest(
    val displayName: String? = null,
    val relationship: String? = null,
    val roomLabel: String? = null
)

data class OneFamilyMemberMutation(
    val member: OneRemoteFamilyMember,
    val invalidatedSessions: Int
)

data class FamilyMemberUpdateRequest(
    val role: OneRole
)

data class OneCameraPairingStatus(
    val pairingId: UUID,
    val homeId: UUID,
    val status: String,
    val expiresAt: Instant?,
    val connectedAt: Instant?,
    val deviceId: UUID?,
    val deviceLabel: String?,
    val deviceRole: String?
)

data class OneCameraReconnectLink(
    val cameraId: UUID,
    val reconnectToken: String
)

/** Pairing response for a camera/microphone publisher device. */
data class PublisherPairingStartRequest(
    val label: String,
    val expiresInSeconds: Long = 600
)

data class PublisherPairingStartResponse(
    val pairingId: UUID,
    val pairingCode: String,
    val expiresInSeconds: Long,
    val homeId: UUID,
    val userId: UUID
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

data class OneRoom(
    val id: UUID,
    val homeId: UUID?,
    val name: String
)

data class OneRoomMap(
    val id: UUID,
    val homeId: UUID?,
    val roomId: UUID?,
    val revision: Int,
    val coordinateFrame: String,
    val zones: List<String>,
    val createdAt: Instant?,
    val source: OneMapSource = OneMapSource.UNKNOWN,
    val provenance: String = "unknown",
    val dimension: OneMapDimension = OneMapDimension.UNKNOWN,
    val approximate: Boolean = true,
    val metricScaleKnown: Boolean = false,
    val scaleMetersPerUnit: Double? = null,
    val localizationStatus: String = "unlocalized",
    val geometryStatus: String = "unknown",
    val rescanRequired: Boolean = false,
    val confidence: Double? = null,
    val modelVersion: String? = null,
    val polygons: List<OneMapPolygon> = emptyList(),
    val walls: List<OneMapWall> = emptyList(),
    val furniture: List<OneMapFurniture> = emptyList(),
    val openings: List<OneMapOpening> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val usdzAvailable: Boolean = false
)

data class OneMapGenerationStartRequest(
    val roomId: UUID? = null,
    val roomLabel: String? = null,
    val orientation: String = "landscape",
    val resolutionWidth: Int = OneRoomSweepCaptureConfig.TARGET_WIDTH,
    val resolutionHeight: Int = OneRoomSweepCaptureConfig.TARGET_HEIGHT
)

data class OneMapGenerationFrame(
    val frameBase64: String,
    val width: Int,
    val height: Int,
    val capturedAt: Instant? = null
)

data class OneMapGeneration(
    val id: UUID,
    val homeId: UUID,
    val cameraId: UUID,
    val roomId: UUID?,
    val roomLabel: String,
    val orientation: String,
    val status: String,
    val progress: Int,
    val source: OneMapSource,
    val dimension: OneMapDimension,
    val metricScaleKnown: Boolean,
    val frameCount: Int,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val mapId: UUID?,
    val errorCode: String?,
    val errorMessage: String?,
    val metrics: Map<String, String>,
    val modelVersion: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val completedAt: Instant?
) {
    val isCollecting: Boolean
        get() = status.equals("collecting", ignoreCase = true)

    val isProcessing: Boolean
        get() = status.equals("processing", ignoreCase = true)

    val isTerminal: Boolean
        get() = status.lowercase() in setOf("ready", "needs_rescan", "unavailable", "failed")
}

data class OneRemoteCamera(
    val id: UUID,
    val name: String,
    val roomId: UUID?,
    val platform: String,
    val status: String,
    val enabled: Boolean,
    val lastSeenAt: Instant?,
    val source: String = OneCameraSource.LEGACY
)

data class CameraRegistrationRequest(
    val name: String,
    val roomId: UUID? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class CameraUpdateRequest(
    val name: String? = null,
    val roomId: UUID? = null,
    val enabled: Boolean? = null
)

data class OneCameraCalibration(
    val id: UUID,
    val cameraId: UUID,
    val mapId: UUID,
    val accuracyM: Double?
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

data class OneVisionDetection(
    val label: String,
    val confidence: Double,
    val boundingBox: List<Double>,
    val zone: String?,
    val worldPoint: List<Double?>?,
    val uncertaintyM: Double?,
    val projectionQuality: String?
)

data class OneVisionFrameResult(
    val detections: List<OneVisionDetection>,
    val detectorVersion: String,
    val persisted: Boolean,
    val privacy: String
)

data class OneObjectRequest(
    val label: String,
    val displayName: String? = null
)

data class OneObservationRequest(
    val objectId: UUID? = null,
    val cameraId: UUID? = null,
    val mapId: UUID? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val uncertaintyM: Double? = null,
    val confidence: Double = 0.0,
    val detectorVersion: String = "android-manual-v1"
)

data class OneObservationResult(
    val observationId: UUID?,
    val eventId: UUID?,
    val approximateLocation: List<Double?>
)

data class OneRemoteEvent(
    val id: UUID,
    val type: String,
    val status: String,
    val explanation: String,
    val confidence: Double,
    val lastSeenAt: Instant?,
    val evidenceIds: List<String> = emptyList()
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

data class OneRemoteMedicationCheckIn(
    val id: UUID?,
    val planId: UUID,
    val subjectUserId: UUID?,
    val scheduledFor: Instant,
    val status: String,
    val note: String?,
    val updatedAt: Instant?
)

data class MedicationPlanRequest(
    val subjectUserId: UUID,
    val name: String,
    val dose: String,
    val schedule: String,
    val instructions: String = "",
    val active: Boolean = true,
    val assignedCaregiverId: UUID? = null
)

data class MedicationPlanUpdateRequest(
    val name: String? = null,
    val dose: String? = null,
    val schedule: String? = null,
    val instructions: String? = null,
    val active: Boolean? = null,
    val assignedCaregiverId: UUID? = null,
    val version: Int? = null
)

data class OneMedicationPlan(
    val id: UUID,
    val homeId: UUID?,
    val subjectUserId: UUID,
    val name: String,
    val dose: String,
    val schedule: String,
    val instructions: String,
    val active: Boolean,
    val version: Int,
    val assignedCaregiverId: UUID?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class OneFamilyAssistantResult(
    val summary: String,
    val nextAction: String,
    val evidenceIds: List<String>,
    val limitations: String,
    val degraded: Boolean,
    val inferenceStatus: String?,
    val modelVersion: String?
)

class OneApiException(message: String, val statusCode: Int? = null, cause: Throwable? = null) : IOException(message, cause)

interface OneApiClient {
    suspend fun health(): BackendHealth
    suspend fun startPairing(pairingRequest: PairingStartRequest, bootstrapSecret: String? = null): PairingStartResponse
    suspend fun startPublisherPairing(session: OneSession, request: PublisherPairingStartRequest): PublisherPairingStartResponse
    suspend fun requestEmailCode(request: EmailAuthRequest): EmailAuthChallenge
    suspend fun verifyEmailCode(request: EmailAuthVerifyRequest): OneSession
    suspend fun completePairing(code: String): OneSession
    suspend fun acceptFamilyInvite(inviteRequest: FamilyInviteAcceptRequest): OneSession
    suspend fun createFamilyInvite(session: OneSession, inviteRequest: FamilyInviteRequest): OneFamilyInvite
    suspend fun careSpaces(session: OneSession): List<OneCareSpace>
    suspend fun createCareSpace(session: OneSession, request: CareSpaceCreateRequest): OneSession
    suspend fun activateCareSpace(session: OneSession, homeId: UUID): OneSession
    suspend fun careRecipients(session: OneSession): List<OneCareRecipient>
    suspend fun createCareRecipient(session: OneSession, request: CareRecipientCreateRequest): OneCareRecipient
    suspend fun updateCareRecipient(session: OneSession, recipientId: UUID, request: CareRecipientUpdateRequest): OneCareRecipient
    suspend fun deleteCareRecipient(session: OneSession, recipientId: UUID): OneCareRecipient
    suspend fun recordConsent(session: OneSession, consentRequest: ConsentRequest)
    suspend fun homeConsents(session: OneSession): List<OneRemoteConsent>
    suspend fun requestDataExport(session: OneSession): OneDataExport
    suspend fun requestDataDeletion(session: OneSession): OneDataDeletion
    suspend fun logout(session: OneSession)
    suspend fun liveKitToken(session: OneSession, mode: String = "subscribe"): OneLiveKitToken
    suspend fun streamHomeEvents(session: OneSession, onEvent: suspend (OneRemoteEventSignal) -> Unit)
    fun clipContentUrl(session: OneSession, clipId: UUID): String
    suspend fun homeProfile(session: OneSession): OneHomeProfile
    suspend fun homeRooms(session: OneSession): List<OneRoom>
    suspend fun createRoom(session: OneSession, name: String): OneRoom
    suspend fun currentRoomMap(session: OneSession): OneRoomMap?
    suspend fun uploadRoomMap(
        session: OneSession,
        roomId: UUID?,
        zones: List<String>,
        coordinateFrame: String = "manual-zones"
    ): OneRoomMap
    suspend fun startMapGeneration(
        session: OneSession,
        cameraId: UUID,
        generationRequest: OneMapGenerationStartRequest
    ): OneMapGeneration
    suspend fun latestMapGeneration(session: OneSession, cameraId: UUID): OneMapGeneration?
    suspend fun mapGeneration(session: OneSession, cameraId: UUID, jobId: UUID): OneMapGeneration
    suspend fun submitMapGenerationFrames(
        session: OneSession,
        cameraId: UUID,
        jobId: UUID,
        frames: List<OneMapGenerationFrame>
    ): OneMapGeneration
    suspend fun homeCameras(session: OneSession): List<OneRemoteCamera>
    suspend fun registerCamera(session: OneSession, request: CameraRegistrationRequest): OneRemoteCamera
    suspend fun updateCamera(session: OneSession, cameraId: UUID, request: CameraUpdateRequest): OneRemoteCamera
    suspend fun createCalibration(
        session: OneSession,
        cameraId: UUID,
        mapId: UUID,
        accuracyM: Double? = null,
        anchorLabels: List<String> = emptyList()
    ): OneCameraCalibration
    suspend fun homeObjects(session: OneSession): List<OneRemoteObject>
    suspend fun ingestVisionFrame(
        session: OneSession,
        cameraId: UUID,
        frameBase64: String,
        width: Int,
        height: Int,
        candidateLabels: List<String>,
        capturedAt: Instant? = null,
        depthM: Double? = null
    ): OneVisionFrameResult
    suspend fun createObject(session: OneSession, request: OneObjectRequest): OneRemoteObject
    suspend fun submitObservation(session: OneSession, request: OneObservationRequest): OneObservationResult
    suspend fun homeEvents(session: OneSession, limit: Int = 50): List<OneRemoteEvent>
    suspend fun homeClips(session: OneSession): List<OneRemoteClip>
    suspend fun familyMembers(session: OneSession): List<OneRemoteFamilyMember>
    suspend fun updateFamilyMember(session: OneSession, userId: UUID, request: FamilyMemberUpdateRequest): OneFamilyMemberMutation
    suspend fun removeFamilyMember(session: OneSession, userId: UUID): OneFamilyMemberMutation
    suspend fun cameraPairingStatus(session: OneSession, pairingId: UUID): OneCameraPairingStatus
    suspend fun reconnectCamera(cameraId: UUID, reconnectToken: String): OneSession
    suspend fun createCameraReconnectLink(session: OneSession): OneCameraReconnectLink
    suspend fun medicationReminders(session: OneSession, day: String? = null, subjectUserId: UUID? = null): List<OneRemoteMedicationReminder>
    suspend fun medicationCheckIns(
        session: OneSession,
        subjectUserId: UUID? = null,
        scheduledFrom: Instant? = null,
        scheduledTo: Instant? = null
    ): List<OneRemoteMedicationCheckIn>
    suspend fun medicationPlans(session: OneSession, subjectUserId: UUID? = null, activeOnly: Boolean = true): List<OneMedicationPlan>
    suspend fun createMedicationPlan(session: OneSession, request: MedicationPlanRequest): OneMedicationPlan
    suspend fun updateMedicationPlan(session: OneSession, planId: UUID, request: MedicationPlanUpdateRequest): OneMedicationPlan
    suspend fun familyAssistant(session: OneSession, message: String, subjectUserId: UUID): OneFamilyAssistantResult
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
    val apiBaseUrl: String
        get() = configuration.apiBaseUrl

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

    override suspend fun startPublisherPairing(
        session: OneSession,
        request: PublisherPairingStartRequest
    ): PublisherPairingStartResponse {
        val label = request.label.trim().take(120)
        require(label.isNotBlank()) { "Enter a device label." }
        val payload = JSONObject()
            .put("label", label)
            .put("expires_in_seconds", request.expiresInSeconds.coerceIn(60, 900))
        val body = request(
            "/homes/${session.homeId}/pairing/start",
            "POST",
            payload,
            token = session.accessToken
        )
        val pairingCode = body.optNullableString("pairing_code") ?: body.requiredString("code")
        return PublisherPairingStartResponse(
            pairingId = body.optNullableUuid("pairing_id") ?: body.requiredUuid("user_id"),
            pairingCode = pairingCode,
            expiresInSeconds = body.optLong("expires_in_seconds", request.expiresInSeconds),
            homeId = body.optNullableUuid("home_id") ?: session.homeId,
            userId = body.requiredUuid("user_id")
        )
    }

    override suspend fun requestEmailCode(request: EmailAuthRequest): EmailAuthChallenge {
        val payload = JSONObject()
            .put("email", request.email.trim())
            .put("purpose", request.purpose)
            .put("home_name", request.homeName.trim().ifBlank { "ONE Home" })
            .put("care_setting", request.careSetting)
            .put("support_focus", request.supportFocus)
            .put("role", request.role.wireValue)
        request.displayName?.trim()?.takeIf { it.isNotBlank() }?.let { payload.put("display_name", it) }
        val body = request(
            "/auth/email/request",
            "POST",
            payload
        )
        return EmailAuthChallenge(
            verificationId = body.requiredUuid("verification_id"),
            expiresInSeconds = body.requiredLong("expires_in_seconds"),
            delivery = body.optString("delivery").takeIf { it.isNotBlank() } ?: "unknown",
            devCode = body.optNullableString("dev_code"),
            email = body.requiredString("email"),
            purpose = body.requiredString("purpose"),
            homeId = body.requiredUuid("home_id"),
            userId = body.requiredUuid("user_id"),
            role = body.optString("role").takeIf { it.isNotBlank() } ?: request.role.wireValue
        )
    }

    override suspend fun verifyEmailCode(request: EmailAuthVerifyRequest): OneSession {
        val payload = JSONObject()
            .put("email", request.email.trim())
            .put("code", request.code.trim())
        return sessionFrom(request("/auth/email/verify", "POST", payload))
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

    override suspend fun careSpaces(session: OneSession): List<OneCareSpace> {
        val rows = request("/account/homes", "GET", token = session.accessToken)
            .optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let { row ->
                    runCatching { parseCareSpace(row) }.getOrNull()?.let(::add)
                }
            }
        }
    }

    override suspend fun createCareSpace(session: OneSession, request: CareSpaceCreateRequest): OneSession {
        val name = request.name.trim().take(120)
        require(name.isNotBlank()) { "Care space name is required." }
        require(request.careSetting in setOf("home", "residence")) { "Care setting is not supported." }
        require(request.supportFocus in setOf("general", "mci")) { "Support focus is not supported." }
        val body = request(
            "/account/homes",
            "POST",
            JSONObject()
                .put("name", name)
                .put("care_setting", request.careSetting)
                .put("support_focus", request.supportFocus),
            token = session.accessToken
        )
        return sessionFrom(body)
    }

    override suspend fun activateCareSpace(session: OneSession, homeId: UUID): OneSession = sessionFrom(
        request(
            "/account/homes/$homeId/activate",
            "POST",
            token = session.accessToken
        )
    )

    override suspend fun careRecipients(session: OneSession): List<OneCareRecipient> {
        val rows = request("/homes/${session.homeId}/care-recipients", "GET", token = session.accessToken)
            .optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let { row ->
                    runCatching { parseCareRecipient(row) }.getOrNull()?.let(::add)
                }
            }
        }
    }

    override suspend fun createCareRecipient(session: OneSession, request: CareRecipientCreateRequest): OneCareRecipient {
        val displayName = request.displayName.trim().take(120)
        require(displayName.isNotBlank()) { "Care recipient name is required." }
        val payload = JSONObject().put("display_name", displayName)
        request.relationship?.trim()?.takeIf { it.isNotBlank() }?.let { payload.put("relationship", it.take(120)) }
        request.roomLabel?.trim()?.takeIf { it.isNotBlank() }?.let { payload.put("room_label", it.take(120)) }
        val body = request(
            "/homes/${session.homeId}/care-recipients",
            "POST",
            payload,
            token = session.accessToken
        )
        return parseCareRecipient(body.optJSONObject("data") ?: body)
    }

    override suspend fun updateCareRecipient(
        session: OneSession,
        recipientId: UUID,
        request: CareRecipientUpdateRequest
    ): OneCareRecipient {
        val payload = JSONObject()
        request.displayName?.let { payload.put("display_name", it.trim().take(120)) }
        if (request.relationship == null) payload.put("relationship", JSONObject.NULL) else payload.put("relationship", request.relationship.trim().take(120))
        if (request.roomLabel == null) payload.put("room_label", JSONObject.NULL) else payload.put("room_label", request.roomLabel.trim().take(120))
        if (payload.length() == 0) throw OneApiException("At least one care-recipient field is required.")
        val body = request(
            "/homes/${session.homeId}/care-recipients/$recipientId",
            "PATCH",
            payload,
            token = session.accessToken
        )
        return parseCareRecipient(body.optJSONObject("data") ?: body)
    }

    override suspend fun deleteCareRecipient(session: OneSession, recipientId: UUID): OneCareRecipient {
        val body = request(
            "/homes/${session.homeId}/care-recipients/$recipientId",
            "DELETE",
            token = session.accessToken
        )
        return parseCareRecipient(body.optJSONObject("data") ?: body)
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
        return OneSession(
            accessToken = accessToken,
            homeId = homeId,
            userId = userId,
            role = role,
            expiresAt = expiresAt,
            backendRole = backendRole,
            reconnectToken = body.optNullableString("reconnect_token")
        )
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

    override suspend fun homeRooms(session: OneSession): List<OneRoom> {
        val rows = request("/homes/${session.homeId}/rooms", "GET", token = session.accessToken).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = runCatching { UUID.fromString(row.optString("id")) }.getOrNull() ?: continue
                val name = row.optString("name").takeIf { it.isNotBlank() } ?: continue
                add(OneRoom(id = id, homeId = row.optNullableUuid("home_id"), name = name))
            }
        }
    }

    override suspend fun createRoom(session: OneSession, name: String): OneRoom {
        val body = request(
            "/homes/${session.homeId}/rooms",
            "POST",
            JSONObject().put("name", name),
            token = session.accessToken
        )
        return OneRoom(
            id = body.requiredUuid("id"),
            homeId = body.optNullableUuid("home_id") ?: session.homeId,
            name = body.requiredString("name")
        )
    }

    override suspend fun currentRoomMap(session: OneSession): OneRoomMap? {
        return try {
            parseRoomMap(
                body = request("/homes/${session.homeId}/maps/current", "GET", token = session.accessToken),
                homeId = session.homeId
            )
        } catch (error: OneApiException) {
            if (error.statusCode == 404) null else throw error
        }
    }

    override suspend fun uploadRoomMap(
        session: OneSession,
        roomId: UUID?,
        zones: List<String>,
        coordinateFrame: String
    ): OneRoomMap {
        val zoneRows = JSONArray()
        zones.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { zoneRows.put(JSONObject().put("label", it)) }
        val mapData = JSONObject()
            .put("source", "android-manual")
            .put("zones", zoneRows)
        val payload = JSONObject()
            .put("room_id", roomId?.toString() ?: JSONObject.NULL)
            .put("coordinate_frame", coordinateFrame)
            .put("map_data", mapData)
        val body = request(
            "/homes/${session.homeId}/maps",
            "POST",
            payload,
            token = session.accessToken
        )
        return parseRoomMap(body, homeId = session.homeId, fallbackRoomId = roomId, fallbackZones = zones)
    }

    override suspend fun startMapGeneration(
        session: OneSession,
        cameraId: UUID,
        generationRequest: OneMapGenerationStartRequest
    ): OneMapGeneration {
        require(generationRequest.resolutionWidth in 1..7_680 && generationRequest.resolutionHeight in 1..4_320) {
            "Room sweep resolution is not supported."
        }
        val payload = JSONObject()
            .put("room_id", generationRequest.roomId?.toString() ?: JSONObject.NULL)
            .put("orientation", generationRequest.orientation.trim().ifBlank { "landscape" }.take(32))
            .put("resolution_width", generationRequest.resolutionWidth)
            .put("resolution_height", generationRequest.resolutionHeight)
        generationRequest.roomLabel?.trim()?.takeIf { it.isNotBlank() }?.let { payload.put("room_label", it.take(120)) }
        return parseMapGeneration(
            body = request(
                "/homes/${session.homeId}/cameras/$cameraId/map-generation",
                "POST",
                payload,
                token = session.accessToken
            ),
            homeId = session.homeId,
            fallbackCameraId = cameraId
        )
    }

    override suspend fun latestMapGeneration(session: OneSession, cameraId: UUID): OneMapGeneration? {
        return try {
            parseMapGeneration(
                body = request(
                    "/homes/${session.homeId}/cameras/$cameraId/map-generation",
                    "GET",
                    token = session.accessToken
                ),
                homeId = session.homeId,
                fallbackCameraId = cameraId
            )
        } catch (error: OneApiException) {
            if (error.statusCode == 404) null else throw error
        }
    }

    override suspend fun mapGeneration(session: OneSession, cameraId: UUID, jobId: UUID): OneMapGeneration =
        parseMapGeneration(
            body = request(
                "/homes/${session.homeId}/cameras/$cameraId/map-generation/$jobId",
                "GET",
                token = session.accessToken
            ),
            homeId = session.homeId,
            fallbackCameraId = cameraId
        )

    override suspend fun submitMapGenerationFrames(
        session: OneSession,
        cameraId: UUID,
        jobId: UUID,
        frames: List<OneMapGenerationFrame>
    ): OneMapGeneration {
        require(frames.size in 3..OneRoomSweepCaptureConfig.MAX_FRAME_COUNT) {
            "A room sweep needs between 3 and ${OneRoomSweepCaptureConfig.MAX_FRAME_COUNT} frames."
        }
        val frameRows = JSONArray()
        frames.forEach { frame ->
            require(frame.frameBase64.isNotBlank() && frame.frameBase64.length <= 4_000_000) {
                "A room sweep frame is empty or too large."
            }
            frameRows.put(
                JSONObject()
                    .put("frame_base64", frame.frameBase64)
                    .put("width", frame.width)
                    .put("height", frame.height)
                    .apply { frame.capturedAt?.let { put("captured_at", it.toString()) } }
            )
        }
        return parseMapGeneration(
            body = request(
                "/homes/${session.homeId}/cameras/$cameraId/map-generation/$jobId/frames",
                "POST",
                JSONObject().put("frames", frameRows),
                token = session.accessToken
            ),
            homeId = session.homeId,
            fallbackCameraId = cameraId
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
                        lastSeenAt = (row.optNullableString("lastSeenAt") ?: row.optNullableString("last_seen_at")).toInstantOrNull(),
                        source = row.cameraSource()
                    )
                )
            }
        }
    }

    override suspend fun registerCamera(session: OneSession, request: CameraRegistrationRequest): OneRemoteCamera {
        val payload = JSONObject().put("name", request.name)
        request.roomId?.let { payload.put("room_id", it.toString()) }
        if (request.metadata.isNotEmpty()) {
            payload.put("metadata", JSONObject().apply { request.metadata.forEach { (key, value) -> put(key, value) } })
        }
        val body = request(
            "/homes/${session.homeId}/cameras",
            "POST",
            payload,
            token = session.accessToken
        )
        return OneRemoteCamera(
            id = body.requiredUuid("id"),
            name = body.optString("name").takeIf { it.isNotBlank() } ?: request.name,
            roomId = body.optNullableUuid("room_id") ?: body.optNullableUuid("roomId") ?: request.roomId,
            platform = body.optString("platform").takeIf { it.isNotBlank() } ?: "browser",
            status = body.optString("status").takeIf { it.isNotBlank() } ?: "online",
            enabled = body.optNullableBoolean("enabled") ?: true,
            lastSeenAt = (body.optNullableString("lastSeenAt") ?: body.optNullableString("last_seen_at")).toInstantOrNull(),
            source = body.cameraSource(default = request.metadata["source"] ?: OneCameraSource.LEGACY)
        )
    }

    override suspend fun updateCamera(
        session: OneSession,
        cameraId: UUID,
        request: CameraUpdateRequest
    ): OneRemoteCamera {
        val payload = JSONObject()
        request.name?.let { payload.put("name", it) }
        if (request.roomId == null) {
            payload.put("room_id", JSONObject.NULL)
        } else {
            payload.put("room_id", request.roomId.toString())
        }
        request.enabled?.let { payload.put("enabled", it) }
        val body = request(
            "/homes/${session.homeId}/cameras/$cameraId",
            "PATCH",
            payload,
            token = session.accessToken
        )
        return OneRemoteCamera(
            id = body.requiredUuid("id"),
            name = body.optString("name").takeIf { it.isNotBlank() } ?: request.name.orEmpty(),
            roomId = body.optNullableUuid("room_id") ?: body.optNullableUuid("roomId"),
            platform = body.optString("platform").takeIf { it.isNotBlank() } ?: "browser",
            status = body.optString("status").takeIf { it.isNotBlank() } ?: "unknown",
            enabled = body.optNullableBoolean("enabled") ?: request.enabled ?: true,
            lastSeenAt = (body.optNullableString("lastSeenAt") ?: body.optNullableString("last_seen_at")).toInstantOrNull(),
            source = body.cameraSource()
        )
    }

    override suspend fun createCalibration(
        session: OneSession,
        cameraId: UUID,
        mapId: UUID,
        accuracyM: Double?,
        anchorLabels: List<String>
    ): OneCameraCalibration {
        val anchors = anchorLabels.map(String::trim).filter(String::isNotBlank).distinct().take(10)
        val intrinsics = JSONObject()
            .put("source", "android-manual")
            .put("calibration_mode", "manual-anchors")
            .put("anchor_labels", JSONArray(anchors))
        val extrinsics = JSONObject()
            .put("source", "android-manual")
            .put("calibration_mode", "manual-anchors")
            .put("coordinate_frame", "map")
            .put("anchor_count", anchors.size)
        val payload = JSONObject()
            .put("camera_id", cameraId.toString())
            .put("map_id", mapId.toString())
            .put("intrinsics", intrinsics)
            .put("extrinsics", extrinsics)
        accuracyM?.let { payload.put("accuracy_m", it) }
        val body = request(
            "/homes/${session.homeId}/calibrations",
            "POST",
            payload,
            token = session.accessToken
        )
        return OneCameraCalibration(
            id = body.requiredUuid("id"),
            cameraId = body.optNullableUuid("camera_id") ?: cameraId,
            mapId = body.optNullableUuid("map_id") ?: mapId,
            accuracyM = body.optNullableDouble("accuracy_m") ?: accuracyM
        )
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

    override suspend fun ingestVisionFrame(
        session: OneSession,
        cameraId: UUID,
        frameBase64: String,
        width: Int,
        height: Int,
        candidateLabels: List<String>,
        capturedAt: Instant?,
        depthM: Double?
    ): OneVisionFrameResult {
        require(width in 1..7_680 && height in 1..4_320) { "Frame dimensions are not supported." }
        require(frameBase64.length <= 4_000_000) { "Frame is too large." }
        val labels = candidateLabels.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(20)
        require(labels.isNotEmpty()) { "At least one candidate label is required." }
        val payload = JSONObject()
            .put("camera_id", cameraId.toString())
            .put("frame_base64", frameBase64)
            .put("width", width)
            .put("height", height)
            .put("candidate_labels", JSONArray(labels))
        capturedAt?.let { payload.put("captured_at", it.toString()) }
        depthM?.let { payload.put("depth_m", it) }
        val body = request(
            "/homes/${session.homeId}/vision/frames",
            "POST",
            payload,
            token = session.accessToken
        )
        val rows = body.optJSONArray("data") ?: JSONArray()
        val detections = buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val box = row.optJSONArray("bbox")?.toDoubleList().orEmpty()
                val projection = row.optJSONObject("projection")
                val world = projection?.optJSONArray("world_xyz")?.toNullableDoubleList()
                add(
                    OneVisionDetection(
                        label = row.optString("label").takeIf { it.isNotBlank() } ?: "unknown",
                        confidence = row.optDouble("confidence", 0.0).coerceIn(0.0, 1.0),
                        boundingBox = box,
                        zone = projection?.optString("zone")?.takeIf { it.isNotBlank() },
                        worldPoint = world,
                        uncertaintyM = projection?.optNullableDouble("uncertainty_m"),
                        projectionQuality = projection?.optString("quality")?.takeIf { it.isNotBlank() }
                    )
                )
            }
        }
        return OneVisionFrameResult(
            detections = detections,
            detectorVersion = body.optString("detector_version").takeIf { it.isNotBlank() } ?: "unknown",
            persisted = body.optBoolean("persisted", false),
            privacy = body.optString("privacy").takeIf { it.isNotBlank() } ?: "Frame processed in memory."
        )
    }

    override suspend fun createObject(session: OneSession, request: OneObjectRequest): OneRemoteObject {
        val label = request.label.trim().take(80)
        require(label.isNotBlank()) { "Object label is required." }
        val payload = JSONObject().put("label", label)
        request.displayName?.trim()?.takeIf { it.isNotBlank() }?.let { payload.put("display_name", it.take(120)) }
        val body = request(
            "/homes/${session.homeId}/objects",
            "POST",
            payload,
            token = session.accessToken
        )
        return OneRemoteObject(
            id = body.requiredUuid("id"),
            label = body.optString("display_name").takeIf { it.isNotBlank() } ?: body.optString("label").ifBlank { label },
            status = "unknown",
            zone = null,
            pointX = null,
            pointY = null,
            lastSeenAt = null,
            confidence = 0.0,
            confidenceRadiusM = 0.0
        )
    }

    override suspend fun submitObservation(session: OneSession, request: OneObservationRequest): OneObservationResult {
        val payload = JSONObject()
        request.objectId?.let { payload.put("object_id", it.toString()) }
        request.cameraId?.let { payload.put("camera_id", it.toString()) }
        request.mapId?.let { payload.put("map_id", it.toString()) }
        request.x?.let { payload.put("x", it) }
        request.y?.let { payload.put("y", it) }
        request.z?.let { payload.put("z", it) }
        request.uncertaintyM?.let { payload.put("uncertainty_m", it.coerceIn(0.0, 100.0)) }
        payload.put("confidence", request.confidence.coerceIn(0.0, 1.0))
        payload.put("detector_version", request.detectorVersion.take(80))
        val body = request(
            "/homes/${session.homeId}/observations",
            "POST",
            payload,
            token = session.accessToken
        )
        val point = body.optJSONObject("approximate_location")
        return OneObservationResult(
            observationId = body.optNullableUuid("observation_id"),
            eventId = body.optNullableUuid("event_id"),
            approximateLocation = listOf(point?.optNullableDouble("x"), point?.optNullableDouble("y"), point?.optNullableDouble("z"))
        )
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
                        lastSeenAt = (row.optNullableString("last_seen_at") ?: row.optNullableString("lastSeenAt")).toInstantOrNull(),
                        evidenceIds = row.evidenceIds()
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

    override suspend fun updateFamilyMember(
        session: OneSession,
        userId: UUID,
        request: FamilyMemberUpdateRequest
    ): OneFamilyMemberMutation {
        require(request.role != OneRole.PUBLISHER) { "Publisher access is managed through camera pairing." }
        val body = request(
            "/homes/${session.homeId}/family/members/$userId",
            "PATCH",
            JSONObject().put("role", request.role.wireValue),
            token = session.accessToken
        )
        return parseFamilyMemberMutation(body)
    }

    override suspend fun removeFamilyMember(session: OneSession, userId: UUID): OneFamilyMemberMutation =
        parseFamilyMemberMutation(
            request(
                "/homes/${session.homeId}/family/members/$userId",
                "DELETE",
                token = session.accessToken
            )
        )

    override suspend fun cameraPairingStatus(session: OneSession, pairingId: UUID): OneCameraPairingStatus {
        val body = request(
            "/homes/${session.homeId}/pairing/$pairingId/status",
            "GET",
            token = session.accessToken
        )
        val device = body.optJSONObject("device")
        return OneCameraPairingStatus(
            pairingId = body.optNullableUuid("pairing_id") ?: pairingId,
            homeId = body.optNullableUuid("home_id") ?: session.homeId,
            status = body.optString("status").takeIf { it.isNotBlank() } ?: "pending",
            expiresAt = body.optNullableString("expires_at")?.toInstantOrNull(),
            connectedAt = body.optNullableString("connected_at")?.toInstantOrNull(),
            deviceId = device?.optNullableUuid("id"),
            deviceLabel = device?.optNullableString("label"),
            deviceRole = device?.optNullableString("role")
        )
    }

    override suspend fun reconnectCamera(cameraId: UUID, reconnectToken: String): OneSession {
        val cleanToken = reconnectToken.trim()
        require(cleanToken.isNotBlank()) { "A camera reconnect token is required." }
        return sessionFrom(
            request(
                "/camera/reconnect",
                "POST",
                JSONObject()
                    .put("camera_id", cameraId.toString())
                    .put("reconnect_token", cleanToken),
            )
        )
    }

    override suspend fun createCameraReconnectLink(session: OneSession): OneCameraReconnectLink {
        val body = request("/camera/reconnect-link", "POST", token = session.accessToken)
        return OneCameraReconnectLink(
            cameraId = body.requiredUuid("camera_id"),
            reconnectToken = body.requiredString("reconnect_token")
        )
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

    override suspend fun medicationCheckIns(
        session: OneSession,
        subjectUserId: UUID?,
        scheduledFrom: Instant?,
        scheduledTo: Instant?
    ): List<OneRemoteMedicationCheckIn> {
        val query = buildList {
            subjectUserId?.let { add("subject_user_id=$it") }
            scheduledFrom?.let { add("scheduled_from=$it") }
            scheduledTo?.let { add("scheduled_to=$it") }
        }.joinToString("&").takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
        val rows = request(
            "/homes/${session.homeId}/medication-check-ins$query",
            "GET",
            token = session.accessToken
        ).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val planId = runCatching { UUID.fromString(row.optString("plan_id")) }.getOrNull() ?: continue
                val scheduledFor = row.optNullableString("scheduled_for")?.toInstantOrNull() ?: continue
                add(
                    OneRemoteMedicationCheckIn(
                        id = row.optNullableUuid("id"),
                        planId = planId,
                        subjectUserId = row.optNullableUuid("subject_user_id"),
                        scheduledFor = scheduledFor,
                        status = row.optString("status").takeIf { it.isNotBlank() } ?: "pending",
                        note = row.optNullableString("note"),
                        updatedAt = row.optNullableString("updated_at")?.toInstantOrNull()
                    )
                )
            }
        }.sortedByDescending { it.scheduledFor }
    }

    override suspend fun medicationPlans(session: OneSession, subjectUserId: UUID?, activeOnly: Boolean): List<OneMedicationPlan> {
        val query = buildList {
            subjectUserId?.let { add("subject_user_id=$it") }
            add("active_only=$activeOnly")
        }.joinToString("&")
        val rows = request(
            "/homes/${session.homeId}/medication-plans?$query",
            "GET",
            token = session.accessToken
        ).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let { row ->
                    runCatching { parseMedicationPlan(row, session.homeId) }.getOrNull()?.let(::add)
                }
            }
        }
    }

    override suspend fun createMedicationPlan(session: OneSession, request: MedicationPlanRequest): OneMedicationPlan {
        val payload = JSONObject()
            .put("subject_user_id", request.subjectUserId.toString())
            .put("name", request.name)
            .put("dose", request.dose)
            .put("schedule", request.schedule)
            .put("instructions", request.instructions)
            .put("active", request.active)
        request.assignedCaregiverId?.let { payload.put("assigned_caregiver_id", it.toString()) }
        return parseMedicationPlan(
            request(
                "/homes/${session.homeId}/medication-plans",
                "POST",
                payload,
                token = session.accessToken
            ),
            session.homeId
        )
    }

    override suspend fun updateMedicationPlan(
        session: OneSession,
        planId: UUID,
        request: MedicationPlanUpdateRequest
    ): OneMedicationPlan {
        val payload = JSONObject()
        request.name?.let { payload.put("name", it) }
        request.dose?.let { payload.put("dose", it) }
        request.schedule?.let { payload.put("schedule", it) }
        request.instructions?.let { payload.put("instructions", it) }
        request.active?.let { payload.put("active", it) }
        // The backend treats an explicit JSON null as “unassign”; always send
        // this field so the UI can remove a previous caregiver assignment.
        payload.put("assigned_caregiver_id", request.assignedCaregiverId?.toString() ?: JSONObject.NULL)
        request.version?.let { payload.put("version", it) }
        return parseMedicationPlan(
            request(
                "/homes/${session.homeId}/medication-plans/$planId",
                "PATCH",
                payload,
                token = session.accessToken
            ),
            session.homeId
        )
    }

    override suspend fun familyAssistant(session: OneSession, message: String, subjectUserId: UUID): OneFamilyAssistantResult {
        val body = request(
            "/homes/${session.homeId}/family-assistant",
            "POST",
            JSONObject()
                .put("message", message.take(1_000))
                .put("subject_user_id", subjectUserId.toString()),
            token = session.accessToken
        )
        val data = body.optJSONObject("data") ?: throw OneApiException("ONE API response is missing the family assistant result.")
        val evidenceRows = data.optJSONArray("evidence_ids")
        val evidenceIds = if (evidenceRows == null) {
            emptyList()
        } else {
            buildList {
                for (index in 0 until evidenceRows.length()) {
                    evidenceRows.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        return OneFamilyAssistantResult(
            summary = data.optString("summary").takeIf { it.isNotBlank() } ?: "No summary was returned.",
            nextAction = data.optString("next_action").takeIf { it.isNotBlank() } ?: "Review the reminder list with the resident or care team.",
            evidenceIds = evidenceIds,
            limitations = data.optString("limitations").takeIf { it.isNotBlank() } ?: "This is an administrative summary, not medical advice.",
            degraded = body.optBoolean("degraded", false),
            inferenceStatus = body.optNullableString("inference_status"),
            modelVersion = body.optNullableString("model_version")
        )
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

    private fun parseCareSpace(body: JSONObject): OneCareSpace {
        val id = body.requiredUuid("id")
        val recipientRows = body.optJSONArray("recipientNames") ?: body.optJSONArray("recipient_names")
        val recipientNames = if (recipientRows == null) {
            emptyList()
        } else {
            buildList {
                for (index in 0 until recipientRows.length()) {
                    recipientRows.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        return OneCareSpace(
            id = id,
            name = body.requiredString("name"),
            careSetting = (body.optString("careSetting").ifBlank { body.optString("care_setting") }).ifBlank { "home" },
            supportFocus = (body.optString("supportFocus").ifBlank { body.optString("support_focus") }).ifBlank { "general" },
            residentName = body.optString("residentName").ifBlank { body.optString("resident_name") }.ifBlank { "Resident" },
            recipientNames = recipientNames,
            recipientCount = body.optInt("recipientCount", body.optInt("recipient_count", recipientNames.size)),
            role = body.optString("role").ifBlank { "caregiver" },
            active = body.optBoolean("active", false)
        )
    }

    private fun parseCareRecipient(body: JSONObject): OneCareRecipient = OneCareRecipient(
        id = body.requiredUuid("id"),
        displayName = body.requiredString("display_name"),
        relationship = body.optNullableString("relationship"),
        roomLabel = body.optNullableString("room_label"),
        createdAt = body.optNullableString("created_at")?.toInstantOrNull()
    )

    private fun parseFamilyMemberMutation(body: JSONObject): OneFamilyMemberMutation {
        val row = body.optJSONObject("data") ?: throw OneApiException("ONE API response is missing the family member.")
        val id = row.requiredUuid("id")
        return OneFamilyMemberMutation(
            member = OneRemoteFamilyMember(
                id = id,
                displayName = row.requiredString("display_name"),
                email = row.optNullableString("email"),
                role = row.optString("role").ifBlank { "member" },
                representationStatus = row.optNullableString("representation_status")
            ),
            invalidatedSessions = body.optInt("invalidated_sessions", 0)
        )
    }

    private fun parseMapGeneration(
        body: JSONObject,
        homeId: UUID,
        fallbackCameraId: UUID? = null
    ): OneMapGeneration {
        val metricsObject = body.optJSONObject("metrics")
        val metrics = metricsObject?.let { json ->
            buildMap {
                json.keys().forEach { key ->
                    when (val value = json.opt(key)) {
                        is String, is Number, is Boolean -> put(key, value.toString())
                    }
                }
            }
        }.orEmpty()
        val jobId = body.optNullableUuid("id") ?: body.requiredUuid("job_id")
        return OneMapGeneration(
            id = jobId,
            homeId = body.optNullableUuid("home_id") ?: homeId,
            cameraId = body.optNullableUuid("camera_id") ?: fallbackCameraId
                ?: throw OneApiException("ONE API response is missing 'camera_id'."),
            roomId = body.optNullableUuid("room_id"),
            roomLabel = body.optString("room_label").ifBlank { "Room" },
            orientation = body.optString("orientation").ifBlank { "portrait" },
            status = body.optString("status").ifBlank { "unknown" },
            progress = body.optInt("progress", 0).coerceIn(0, 100),
            source = OneMapSource.fromWire(body.optString("source")),
            dimension = OneMapDimension.fromWire(body.optString("dimension")),
            metricScaleKnown = body.optBoolean("metric_scale_known", false),
            frameCount = body.optInt("frame_count", 0).coerceAtLeast(0),
            resolutionWidth = body.optInt("resolution_width", 0).coerceAtLeast(0),
            resolutionHeight = body.optInt("resolution_height", 0).coerceAtLeast(0),
            mapId = body.optNullableUuid("map_id"),
            errorCode = body.optNullableString("error_code"),
            errorMessage = body.optNullableString("error_message")
                ?: body.optNullableString("error"),
            metrics = metrics,
            modelVersion = body.optNullableString("model_version"),
            createdAt = body.optNullableString("created_at")?.toInstantOrNull(),
            updatedAt = body.optNullableString("updated_at")?.toInstantOrNull(),
            completedAt = body.optNullableString("completed_at")?.toInstantOrNull()
        )
    }

    private fun parseRoomMap(
        body: JSONObject,
        homeId: UUID,
        fallbackRoomId: UUID? = null,
        fallbackZones: List<String> = emptyList()
    ): OneRoomMap {
        val mapData = body.optJSONObject("map_data")
        val geometry = mapData?.optJSONObject("geometry")
            ?: body.optJSONObject("geometry")
        val zoneRows = mapData?.optJSONArray("zones")
            ?: geometry?.optJSONArray("zones")
            ?: body.optJSONArray("zones")
        val zones = if (zoneRows == null) {
            fallbackZones.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        } else {
            buildList {
                for (index in 0 until zoneRows.length()) {
                    when (val value = zoneRows.opt(index)) {
                        is JSONObject -> value.optString("label").takeIf { it.isNotBlank() }
                            ?.let(::add)
                        is String -> value.takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }
        }
        val sourceName = body.optString("source").ifBlank { mapData?.optString("source").orEmpty() }
        val dimensionName = body.optString("dimension").ifBlank { mapData?.optString("dimension").orEmpty() }
        val metadataObject = body.optJSONObject("metadata")
        val metadata = metadataObject?.let { metadataJson ->
            buildMap {
                metadataJson.keys().forEach { key ->
                    val value = metadataJson.opt(key)
                    when (value) {
                        is String, is Number, is Boolean -> put(key, value.toString())
                    }
                }
            }
        }.orEmpty()
        val scale = body.optJSONObject("scale")
            ?: mapData?.optJSONObject("scale")
            ?: geometry?.optJSONObject("scale")
        val scaleMetersPerUnit = scale?.optNullableDouble("meters_per_normalized_unit")
            ?: scale?.optNullableDouble("metersPerNormalizedUnit")
        val polygonRows = geometry?.optJSONArray("polygons")
            ?: geometry?.optJSONArray("rooms")
            ?: mapData?.optJSONArray("polygons")
            ?: body.optJSONArray("polygons")
        val wallRows = geometry?.optJSONArray("walls") ?: mapData?.optJSONArray("walls") ?: body.optJSONArray("walls")
        val furnitureRows = geometry?.optJSONArray("furniture") ?: mapData?.optJSONArray("furniture") ?: body.optJSONArray("furniture")
        val openingRows = geometry?.optJSONArray("openings") ?: mapData?.optJSONArray("openings") ?: body.optJSONArray("openings")
        return OneRoomMap(
            id = body.requiredUuid("id"),
            homeId = body.optNullableUuid("home_id") ?: homeId,
            roomId = body.optNullableUuid("room_id") ?: fallbackRoomId,
            revision = body.optInt("revision", 0),
            coordinateFrame = body.optString("coordinate_frame").takeIf { it.isNotBlank() } ?: "unknown",
            zones = zones,
            createdAt = body.optNullableString("created_at")?.toInstantOrNull(),
            source = OneMapSource.fromWire(sourceName),
            provenance = body.optString("provenance").ifBlank { sourceName.ifBlank { "unknown" } },
            dimension = OneMapDimension.fromWire(dimensionName),
            approximate = body.optBoolean("approximate", true),
            metricScaleKnown = body.optBoolean("metric_scale_known", dimensionName == "3d"),
            scaleMetersPerUnit = scaleMetersPerUnit,
            localizationStatus = body.optString("localization_status").ifBlank { "unlocalized" },
            geometryStatus = body.optString("geometry_status").ifBlank { "unknown" },
            rescanRequired = body.optBoolean("rescan_required", false),
            confidence = body.optNullableDouble("confidence")
                ?: mapData?.optNullableDouble("confidence")
                ?: geometry?.optNullableDouble("confidence"),
            modelVersion = body.optNullableString("model_version")
                ?: metadata["model_version"],
            polygons = polygonRows.parseMapPolygons(),
            walls = wallRows.parseMapWalls(),
            furniture = furnitureRows.parseMapFurniture(),
            openings = openingRows.parseMapOpenings(),
            metadata = metadata,
            usdzAvailable = body.optJSONObject("usdz")?.optBoolean("available", true) == true
        )
    }

    private fun JSONArray?.parseMapPolygons(): List<OneMapPolygon> = this?.let { rows ->
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val points = (row.optJSONArray("points") ?: row.optJSONArray("polygon"))
                    .parseMapPoints()
                if (points.size < 3) continue
                add(
                    OneMapPolygon(
                        id = row.optString("id").ifBlank { "polygon-$index" },
                        label = row.optString("label").ifBlank { row.optString("name").ifBlank { "Room area" } },
                        points = points,
                        confidence = row.optNullableDouble("confidence")?.toFloat()
                    )
                )
            }
        }
    } ?: emptyList()

    private fun JSONArray?.parseMapWalls(): List<OneMapWall> = this?.let { rows ->
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val start = row.optJSONObject("start").parseMapPoint()
                val end = row.optJSONObject("end").parseMapPoint()
                if (start == null || end == null) continue
                add(
                    OneMapWall(
                        id = row.optString("id").ifBlank { "wall-$index" },
                        start = start,
                        end = end,
                        confidence = row.optNullableDouble("confidence")?.toFloat()
                    )
                )
            }
        }
    } ?: emptyList()

    private fun JSONArray?.parseMapFurniture(): List<OneMapFurniture> = this?.let { rows ->
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val center = row.optJSONObject("center").parseMapPoint() ?: continue
                val size = row.optJSONObject("size").parseMapPoint() ?: continue
                add(
                    OneMapFurniture(
                        id = row.optString("id").ifBlank { "furniture-$index" },
                        label = row.optString("label").ifBlank { "Furniture" },
                        center = center,
                        size = size,
                        rotationDegrees = row.optDouble("rotation_degrees", row.optDouble("rotationDegrees", 0.0)).toFloat(),
                        confidence = row.optNullableDouble("confidence")?.toFloat()
                    )
                )
            }
        }
    } ?: emptyList()

    private fun JSONArray?.parseMapOpenings(): List<OneMapOpening> = this?.let { rows ->
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val start = row.optJSONObject("start").parseMapPoint()
                val end = row.optJSONObject("end").parseMapPoint()
                if (start == null || end == null) continue
                add(
                    OneMapOpening(
                        id = row.optString("id").ifBlank { "opening-$index" },
                        kind = row.optString("kind").ifBlank { "opening" },
                        start = start,
                        end = end,
                        confidence = row.optNullableDouble("confidence")?.toFloat()
                    )
                )
            }
        }
    } ?: emptyList()

    private fun JSONArray?.parseMapPoints(): List<OneMapPoint> = this?.let { rows ->
        buildList {
            for (index in 0 until rows.length()) {
                val point = when (val raw = rows.opt(index)) {
                    is JSONObject -> raw.parseMapPoint()
                    is JSONArray -> {
                        val x = raw.optDouble(0, Double.NaN)
                        val y = raw.optDouble(1, Double.NaN)
                        if (x.isFinite() && y.isFinite()) OneMapPoint(x.toFloat(), y.toFloat()) else null
                    }
                    else -> null
                }
                point?.let(::add)
            }
        }
    } ?: emptyList()

    private fun JSONObject?.parseMapPoint(): OneMapPoint? {
        val value = this ?: return null
        val x = value.optDouble("x", Double.NaN)
        val y = value.optDouble("y", Double.NaN)
        return if (x.isFinite() && y.isFinite()) OneMapPoint(x.toFloat(), y.toFloat()) else null
    }

    private fun parseMedicationPlan(body: JSONObject, homeId: UUID): OneMedicationPlan = OneMedicationPlan(
        id = body.requiredUuid("id"),
        homeId = body.optNullableUuid("home_id") ?: homeId,
        subjectUserId = body.requiredUuid("subject_user_id"),
        name = body.requiredString("name"),
        dose = body.requiredString("dose"),
        schedule = body.requiredString("schedule"),
        instructions = body.optString("instructions"),
        active = body.optNullableBoolean("active") ?: true,
        version = body.optInt("version", 1),
        assignedCaregiverId = body.optNullableUuid("assigned_caregiver_id"),
        createdAt = body.optNullableString("created_at")?.toInstantOrNull(),
        updatedAt = body.optNullableString("updated_at")?.toInstantOrNull()
    )

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
    get() = when (this) {
        OneRole.RESIDENT -> "resident"
        OneRole.PUBLISHER -> "publisher"
        OneRole.CAREGIVER -> "caregiver"
    }

internal fun String.toOneRole(): OneRole = when {
    equals("resident", ignoreCase = true) -> OneRole.RESIDENT
    equals("publisher", ignoreCase = true) -> OneRole.PUBLISHER
    else -> OneRole.CAREGIVER
}

private fun JSONObject.requiredString(key: String): String = optString(key).takeIf { it.isNotBlank() } ?: throw OneApiException("ONE API response is missing '$key'.")

private fun JSONObject.requiredLong(key: String): Long = if (has(key) && !isNull(key)) optLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: throw OneApiException("ONE API response has an invalid '$key'.") else throw OneApiException("ONE API response is missing '$key'.")

private fun JSONObject.requiredUuid(key: String): UUID = runCatching { UUID.fromString(requiredString(key)) }.getOrElse { throw OneApiException("ONE API response has an invalid '$key'.", cause = it) }

private fun JSONObject.optNullableString(key: String): String? = optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.optNullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

private fun JSONObject.optNullableUuid(key: String): UUID? = optNullableString(key)?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }

private fun JSONObject.cameraSource(default: String = OneCameraSource.LEGACY): String =
    optNullableString("source")
        ?: optJSONObject("metadata")?.optNullableString("source")
        ?: default

private fun JSONObject.evidenceIds(): List<String> {
    val array = optJSONArray("evidence_ids") ?: optString("evidence_ids")
        .takeIf { it.isNotBlank() && it != "null" }
        ?.let { raw -> runCatching { JSONArray(raw) }.getOrNull() }
        ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun JSONObject.optNullableBoolean(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> null
    }
}

private fun JSONArray.toDoubleList(): List<Double> = buildList {
    for (index in 0 until length()) {
        val value = optDouble(index, Double.NaN)
        if (!value.isNaN()) add(value)
    }
}

private fun JSONArray.toNullableDoubleList(): List<Double?> = buildList {
    for (index in 0 until length()) {
        if (isNull(index)) add(null) else add(optDouble(index, Double.NaN).takeUnless { it.isNaN() })
    }
}

private fun String?.toInstantOrNull(): Instant? = this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }

private fun String.problemMessage(): String = runCatching { JSONObject(this).optString("detail").takeIf { it.isNotBlank() } ?: JSONObject(this).optJSONObject("error")?.optString("message") }.getOrNull() ?: "Request failed."
