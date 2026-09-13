package com.one.cognitivecompanion

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.UUID
/**
 * Compose-observable state and session actions for the top-level ONE flow.
 * Screens remain focused on rendering while this object owns persistence and
 * backend side effects.
 */
@Stable
class OneAppState(
    val apiClient: OneApiClient,
    private val secureStore: OneSecureStore,
    private val homeRepository: OneHomeRepository,
    private val cameraRepository: OneCameraRepository,
    private val familyRepository: OneFamilyRepository,
    private val medicationRepository: OneMedicationRepository,
    private val clipRepository: OneClipRepository
) {
    var authStageName by mutableStateOf(AuthStage.AUTHENTICATED.name)
    var roleName by mutableStateOf(OneRole.CAREGIVER.name)
    var selectedTab by mutableStateOf("home")
    var onboardingStep by mutableStateOf(0)
    var onboardingConsentRoom by mutableStateOf(false)
    var onboardingConsentMic by mutableStateOf(false)
    var onboardingConsentMedication by mutableStateOf(false)
    var onboardingConsentFamily by mutableStateOf(false)
    var backendMode by mutableStateOf(false)
    var backendHealth by mutableStateOf<BackendHealth?>(null)
    var backendHealthLoadState by mutableStateOf(OneBackendHealthLoadState.IDLE)
    var backendHealthError by mutableStateOf<String?>(null)
    var session by mutableStateOf<OneSession?>(null)
    var homeSnapshot by mutableStateOf<OneHomeSnapshot?>(null)
    var homeLoadState by mutableStateOf(OneHomeLoadState.IDLE)
    var homeLoadError by mutableStateOf<String?>(null)
    var rooms by mutableStateOf<List<OneRoom>?>(null)
    var currentRoomMap by mutableStateOf<OneRoomMap?>(null)
    var mapLoadState by mutableStateOf(OneMapLoadState.IDLE)
    var mapLoadError by mutableStateOf<String?>(null)
    var eventStreamState by mutableStateOf(OneEventStreamState.IDLE)
    var eventStreamError by mutableStateOf<String?>(null)
    var cameras by mutableStateOf<List<OneCamera>?>(null)
    var cameraLoadState by mutableStateOf(OneCameraLoadState.IDLE)
    var cameraLoadError by mutableStateOf<String?>(null)
    var cameraActionState by mutableStateOf(OneCameraActionState.IDLE)
    var lastRegisteredCamera by mutableStateOf<OneCamera?>(null)
    var cameraActionError by mutableStateOf<String?>(null)
    var familyMembers by mutableStateOf<List<OneFamilyMember>?>(null)
    var familyLoadState by mutableStateOf(OneFamilyLoadState.IDLE)
    var familyLoadError by mutableStateOf<String?>(null)
    var familyInviteLoadState by mutableStateOf(OneFamilyInviteLoadState.IDLE)
    var familyInvite by mutableStateOf<OneFamilyInvite?>(null)
    var familyInviteLoadError by mutableStateOf<String?>(null)
    var selectedFamilySubjectId by mutableStateOf<UUID?>(null)
    var familySubjectInitialized by mutableStateOf(false)
    var medicationDoses by mutableStateOf<List<MedicationDose>?>(null)
    var medicationLoadState by mutableStateOf(OneMedicationLoadState.IDLE)
    var medicationLoadError by mutableStateOf<String?>(null)
    var medicationPlans by mutableStateOf<List<OneMedicationPlan>?>(null)
    var medicationPlansLoadState by mutableStateOf(OneMedicationLoadState.IDLE)
    var medicationPlansLoadError by mutableStateOf<String?>(null)
    var medicationPlanActionState by mutableStateOf(OneMedicationPlanActionState.IDLE)
    var lastMedicationPlan by mutableStateOf<OneMedicationPlan?>(null)
    var medicationPlanActionError by mutableStateOf<String?>(null)
    var medicationActionKey by mutableStateOf<String?>(null)
    var medicationActionError by mutableStateOf<String?>(null)
    var assistantLoadState by mutableStateOf(OneAssistantLoadState.IDLE)
    var assistantResult by mutableStateOf<OneCheckInResult?>(null)
    var assistantLoadError by mutableStateOf<String?>(null)
    var familyAssistantLoadState by mutableStateOf(OneFamilyAssistantLoadState.IDLE)
    var familyAssistantResult by mutableStateOf<OneFamilyAssistantResult?>(null)
    var familyAssistantLoadError by mutableStateOf<String?>(null)
    var consentStates by mutableStateOf<Map<String, Boolean>?>(null)
    var consentLoadState by mutableStateOf(OneConsentLoadState.IDLE)
    var consentLoadError by mutableStateOf<String?>(null)
    var consentUpdatePurpose by mutableStateOf<String?>(null)
    var consentUpdateError by mutableStateOf<String?>(null)
    var exportLoadState by mutableStateOf(OneExportLoadState.IDLE)
    var dataExport by mutableStateOf<OneDataExport?>(null)
    var exportLoadError by mutableStateOf<String?>(null)
    var deletionLoadState by mutableStateOf(OneDeletionLoadState.IDLE)
    var dataDeletion by mutableStateOf<OneDataDeletion?>(null)
    var deletionLoadError by mutableStateOf<String?>(null)
    var clips by mutableStateOf<List<OneClip>?>(null)
    var clipLoadState by mutableStateOf(OneClipLoadState.IDLE)
    var clipLoadError by mutableStateOf<String?>(null)

    val isAdmin: Boolean
        get() = session?.backendRole?.equals("admin", ignoreCase = true) == true

    val canManageFamily: Boolean
        get() = backendMode && session?.backendRole?.let { role ->
            role.equals("admin", ignoreCase = true) || role.equals("caregiver", ignoreCase = true)
        } == true

    suspend fun restoreSession() {
        val restored = withContext(Dispatchers.IO) { secureStore.restore() } ?: return
        session = restored.session
        backendMode = true
        roleName = restored.session.role.name
        selectedTab = if (restored.session.role == OneRole.CAREGIVER) "home" else "today"
        if (restored.onboardingComplete) {
            onboardingStep = 3
            authStageName = AuthStage.AUTHENTICATED.name
        } else {
            onboardingConsentRoom = false
            onboardingConsentMic = false
            onboardingConsentMedication = false
            onboardingConsentFamily = false
            onboardingStep = 0
            authStageName = AuthStage.ONBOARDING.name
        }
    }

    fun applyAuthenticatedSession(authenticatedSession: OneSession?, usedBackend: Boolean) {
        session = authenticatedSession
        backendMode = usedBackend
        backendHealth = null
        backendHealthLoadState = OneBackendHealthLoadState.IDLE
        backendHealthError = null
        homeSnapshot = null
        homeLoadState = OneHomeLoadState.IDLE
        homeLoadError = null
        rooms = null
        currentRoomMap = null
        mapLoadState = OneMapLoadState.IDLE
        mapLoadError = null
        eventStreamState = OneEventStreamState.IDLE
        eventStreamError = null
        cameras = null
        cameraLoadState = OneCameraLoadState.IDLE
        cameraLoadError = null
        cameraActionState = OneCameraActionState.IDLE
        lastRegisteredCamera = null
        cameraActionError = null
        familyMembers = null
        familyLoadState = OneFamilyLoadState.IDLE
        familyLoadError = null
        familyInviteLoadState = OneFamilyInviteLoadState.IDLE
        familyInvite = null
        familyInviteLoadError = null
        selectedFamilySubjectId = null
        familySubjectInitialized = false
        medicationDoses = null
        medicationLoadState = OneMedicationLoadState.IDLE
        medicationLoadError = null
        medicationPlans = null
        medicationPlansLoadState = OneMedicationLoadState.IDLE
        medicationPlansLoadError = null
        medicationPlanActionState = OneMedicationPlanActionState.IDLE
        lastMedicationPlan = null
        medicationPlanActionError = null
        medicationActionKey = null
        medicationActionError = null
        assistantLoadState = OneAssistantLoadState.IDLE
        assistantResult = null
        assistantLoadError = null
        familyAssistantLoadState = OneFamilyAssistantLoadState.IDLE
        familyAssistantResult = null
        familyAssistantLoadError = null
        consentStates = null
        consentLoadState = OneConsentLoadState.IDLE
        consentLoadError = null
        consentUpdatePurpose = null
        consentUpdateError = null
        exportLoadState = OneExportLoadState.IDLE
        dataExport = null
        exportLoadError = null
        deletionLoadState = OneDeletionLoadState.IDLE
        dataDeletion = null
        deletionLoadError = null
        clips = null
        clipLoadState = OneClipLoadState.IDLE
        clipLoadError = null
        if (usedBackend && authenticatedSession != null) {
            runCatching { secureStore.saveSession(authenticatedSession, onboardingComplete = false) }
        }
        roleName = authenticatedSession?.role?.name ?: OneRole.CAREGIVER.name
        onboardingStep = 0
        authStageName = AuthStage.ONBOARDING.name
    }

    suspend fun recordOnboardingConsents() {
        val authenticatedSession = session ?: return
        if (!backendMode) return
        val choices = listOf(
            "audio_capture" to onboardingConsentMic,
            "video_capture" to onboardingConsentRoom,
            "medication_management" to onboardingConsentMedication,
            "family_mode" to onboardingConsentFamily
        )
        choices.forEach { (purpose, granted) ->
            apiClient.recordConsent(
                session = authenticatedSession,
                consentRequest = ConsentRequest(
                    purpose = purpose,
                    policyVersion = "2026-09",
                    granted = granted
                )
            )
        }
    }

    suspend fun completeOnboarding() {
        val authenticatedSession = session
        if (backendMode && authenticatedSession != null) {
            withContext(Dispatchers.IO) { secureStore.markOnboardingComplete(authenticatedSession) }
        }
        onboardingStep = 3
        authStageName = AuthStage.AUTHENTICATED.name
        selectedTab = if (roleName == OneRole.RESIDENT.name) "today" else "home"
    }

    suspend fun checkBackendHealth() {
        if (!backendMode) {
            backendHealth = null
            backendHealthLoadState = OneBackendHealthLoadState.IDLE
            backendHealthError = null
            return
        }
        backendHealthLoadState = OneBackendHealthLoadState.LOADING
        backendHealthError = null
        try {
            backendHealth = apiClient.health()
            backendHealthLoadState = OneBackendHealthLoadState.LOADED
        } catch (error: Exception) {
            backendHealthLoadState = OneBackendHealthLoadState.ERROR
            backendHealthError = error.message ?: "Could not reach the ONE backend."
        }
    }

    suspend fun loadHome() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            homeSnapshot = null
            homeLoadState = OneHomeLoadState.IDLE
            homeLoadError = null
            return
        }
        homeLoadState = OneHomeLoadState.LOADING
        homeLoadError = null
        try {
            homeSnapshot = homeRepository.load(authenticatedSession)
            homeLoadState = OneHomeLoadState.LOADED
        } catch (error: Exception) {
            homeLoadState = OneHomeLoadState.ERROR
            homeLoadError = error.message ?: "Could not load the household."
        }
    }

    suspend fun loadCameras() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            cameras = null
            cameraLoadState = OneCameraLoadState.IDLE
            cameraLoadError = null
            return
        }
        cameraLoadState = OneCameraLoadState.LOADING
        cameraLoadError = null
        try {
            cameras = cameraRepository.load(authenticatedSession)
            runCatching { apiClient.homeRooms(authenticatedSession) }
                .onSuccess { rooms = it }
            cameraLoadState = OneCameraLoadState.LOADED
        } catch (error: Exception) {
            cameraLoadState = OneCameraLoadState.ERROR
            cameraLoadError = error.message ?: "Could not load the household cameras."
        }
    }

    suspend fun registerCamera(name: String, roomId: UUID?) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Connect a backend session before registering a camera."
            return
        }
        if (!canManageFamily) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Only caregivers can register household cameras."
            return
        }
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Enter a camera name."
            return
        }
        if (roomId != null && rooms?.none { it.id == roomId } == true) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Select a room from this household."
            return
        }
        cameraActionState = OneCameraActionState.SUBMITTING
        cameraActionError = null
        try {
            val remote = apiClient.registerCamera(
                authenticatedSession,
                CameraRegistrationRequest(name = cleanName, roomId = roomId)
            )
            val camera = OneCamera(
                id = remote.id,
                name = remote.name,
                roomId = remote.roomId,
                platform = remote.platform,
                status = remote.status,
                enabled = remote.enabled,
                lastSeenAt = remote.lastSeenAt
            )
            cameras = (cameras.orEmpty().filterNot { it.id == camera.id } + camera).sortedBy { it.name.lowercase() }
            lastRegisteredCamera = camera
            cameraActionState = OneCameraActionState.LOADED
        } catch (error: Exception) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = error.message ?: "Could not register the camera."
        }
    }

    suspend fun loadRoomMap() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            rooms = null
            currentRoomMap = null
            mapLoadState = OneMapLoadState.IDLE
            mapLoadError = null
            return
        }
        mapLoadState = OneMapLoadState.LOADING
        mapLoadError = null
        try {
            rooms = apiClient.homeRooms(authenticatedSession)
            currentRoomMap = apiClient.currentRoomMap(authenticatedSession)
            mapLoadState = OneMapLoadState.LOADED
        } catch (error: Exception) {
            mapLoadState = OneMapLoadState.ERROR
            mapLoadError = error.message ?: "Could not load the room map."
        }
    }

    suspend fun createManualRoomMap(roomName: String, zoneNames: List<String>) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            mapLoadState = OneMapLoadState.ERROR
            mapLoadError = "Connect a backend session before updating the room map."
            return
        }
        val cleanRoomName = roomName.trim()
        val cleanZones = zoneNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanRoomName.isBlank()) {
            mapLoadState = OneMapLoadState.ERROR
            mapLoadError = "Enter a room name."
            return
        }
        if (cleanZones.isEmpty()) {
            mapLoadState = OneMapLoadState.ERROR
            mapLoadError = "Enter at least one zone."
            return
        }
        mapLoadState = OneMapLoadState.SUBMITTING
        mapLoadError = null
        try {
            val room = rooms.orEmpty().firstOrNull { it.name.equals(cleanRoomName, ignoreCase = true) }
                ?: apiClient.createRoom(authenticatedSession, cleanRoomName)
            currentRoomMap = apiClient.uploadRoomMap(authenticatedSession, room.id, cleanZones)
            rooms = (rooms.orEmpty().filterNot { it.id == room.id } + room).sortedBy { it.name.lowercase() }
            mapLoadState = OneMapLoadState.LOADED
            loadHome()
        } catch (error: Exception) {
            mapLoadState = OneMapLoadState.ERROR
            mapLoadError = error.message ?: "Could not save the room map."
        }
    }

    suspend fun observeHomeEvents() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            eventStreamState = OneEventStreamState.IDLE
            eventStreamError = null
            return
        }
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                eventStreamState = OneEventStreamState.CONNECTING
                try {
                    apiClient.streamHomeEvents(authenticatedSession) { signal ->
                        when (signal.eventName) {
                            "one.connected.v1", "one.heartbeat.v1" -> {
                                eventStreamState = OneEventStreamState.CONNECTED
                                eventStreamError = null
                            }
                            "one.event.v1" -> {
                                eventStreamState = OneEventStreamState.CONNECTED
                                eventStreamError = null
                                loadHome()
                            }
                        }
                    }
                    if (currentCoroutineContext().isActive) delay(1_000)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    eventStreamState = OneEventStreamState.ERROR
                    eventStreamError = error.message ?: "Live event updates are unavailable."
                    delay(5_000)
                }
            }
        } catch (error: CancellationException) {
            eventStreamState = OneEventStreamState.IDLE
            throw error
        }
    }

    suspend fun loadFamily() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            familyMembers = null
            familyLoadState = OneFamilyLoadState.IDLE
            familyLoadError = null
            return
        }
        familyLoadState = OneFamilyLoadState.LOADING
        familyLoadError = null
        try {
            familyMembers = familyRepository.load(authenticatedSession)
            if (!familySubjectInitialized) {
                selectedFamilySubjectId = familyMembers.orEmpty()
                    .firstOrNull { it.role.equals("resident", ignoreCase = true) }
                    ?.id
                    ?: authenticatedSession.userId
                familySubjectInitialized = true
            }
            familyLoadState = OneFamilyLoadState.LOADED
        } catch (error: Exception) {
            familyLoadState = OneFamilyLoadState.ERROR
            familyLoadError = error.message ?: "Could not load the care circle."
        }
    }

    suspend fun selectFamilySubject(subjectUserId: UUID) {
        if (!backendMode || session == null) return
        selectedFamilySubjectId = subjectUserId
        familySubjectInitialized = true
        medicationDoses = null
        medicationActionError = null
        familyAssistantLoadState = OneFamilyAssistantLoadState.IDLE
        familyAssistantResult = null
        familyAssistantLoadError = null
        loadMedicationReminders()
        loadMedicationPlans()
    }

    suspend fun createFamilyInvite(inviteRequest: FamilyInviteRequest) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            familyInviteLoadState = OneFamilyInviteLoadState.ERROR
            familyInviteLoadError = "Connect a backend session before inviting someone."
            return
        }
        if (!canManageFamily) {
            familyInviteLoadState = OneFamilyInviteLoadState.ERROR
            familyInviteLoadError = "Only caregivers can create family invitations."
            return
        }
        val displayName = inviteRequest.displayName.trim()
        if (displayName.isBlank()) {
            familyInviteLoadState = OneFamilyInviteLoadState.ERROR
            familyInviteLoadError = "Enter the invited person's name."
            return
        }
        familyInviteLoadState = OneFamilyInviteLoadState.SUBMITTING
        familyInviteLoadError = null
        try {
            familyInvite = apiClient.createFamilyInvite(
                authenticatedSession,
                inviteRequest.copy(
                    displayName = displayName,
                    email = inviteRequest.email?.trim()?.takeIf { it.isNotBlank() }
                )
            )
            familyInviteLoadState = OneFamilyInviteLoadState.LOADED
        } catch (error: Exception) {
            familyInviteLoadState = OneFamilyInviteLoadState.ERROR
            familyInviteLoadError = error.message ?: "Could not create the family invitation."
        }
    }

    suspend fun loadMedicationReminders(subjectUserId: UUID? = selectedFamilySubjectId) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            medicationDoses = null
            medicationLoadState = OneMedicationLoadState.IDLE
            medicationLoadError = null
            return
        }
        medicationLoadState = OneMedicationLoadState.LOADING
        medicationLoadError = null
        try {
            medicationDoses = medicationRepository.load(authenticatedSession, subjectUserId)
            medicationLoadState = OneMedicationLoadState.LOADED
        } catch (error: Exception) {
            medicationLoadState = OneMedicationLoadState.ERROR
            medicationLoadError = error.message ?: "Could not load medication reminders."
        }
    }

    suspend fun loadMedicationPlans(subjectUserId: UUID? = selectedFamilySubjectId) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            medicationPlans = null
            medicationPlansLoadState = OneMedicationLoadState.IDLE
            medicationPlansLoadError = null
            return
        }
        medicationPlansLoadState = OneMedicationLoadState.LOADING
        medicationPlansLoadError = null
        try {
            medicationPlans = apiClient.medicationPlans(authenticatedSession, subjectUserId = subjectUserId)
            medicationPlansLoadState = OneMedicationLoadState.LOADED
        } catch (error: Exception) {
            medicationPlansLoadState = OneMedicationLoadState.ERROR
            medicationPlansLoadError = error.message ?: "Could not load medication plans."
        }
    }

    suspend fun createMedicationPlan(name: String, dose: String, schedule: String, instructions: String) {
        val authenticatedSession = session
        val subjectUserId = selectedFamilySubjectId
        if (!backendMode || authenticatedSession == null) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Connect a backend session before creating a medication plan."
            return
        }
        if (!canManageFamily) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Only caregivers can create medication plans."
            return
        }
        if (subjectUserId == null) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Select a person before creating a medication plan."
            return
        }
        val cleanName = name.trim()
        val cleanDose = dose.trim()
        val cleanSchedule = schedule.trim()
        val cleanInstructions = instructions.trim()
        if (cleanName.isBlank() || cleanDose.isBlank() || cleanSchedule.isBlank()) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Name, dose and schedule are required."
            return
        }
        medicationPlanActionState = OneMedicationPlanActionState.SUBMITTING
        medicationPlanActionError = null
        try {
            lastMedicationPlan = apiClient.createMedicationPlan(
                authenticatedSession,
                MedicationPlanRequest(
                    subjectUserId = subjectUserId,
                    name = cleanName,
                    dose = cleanDose,
                    schedule = cleanSchedule,
                    instructions = cleanInstructions
                )
            )
            medicationPlanActionState = OneMedicationPlanActionState.LOADED
            loadMedicationReminders(subjectUserId)
            loadMedicationPlans(subjectUserId)
        } catch (error: Exception) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = error.message ?: "Could not create the medication plan."
        }
    }

    suspend fun updateMedicationPlan(
        plan: OneMedicationPlan,
        name: String,
        dose: String,
        schedule: String,
        instructions: String,
        active: Boolean
    ) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Connect a backend session before updating a medication plan."
            return
        }
        if (!canManageFamily) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Only caregivers can update medication plans."
            return
        }
        val cleanName = name.trim()
        val cleanDose = dose.trim()
        val cleanSchedule = schedule.trim()
        val cleanInstructions = instructions.trim()
        if (cleanName.isBlank() || cleanDose.isBlank() || cleanSchedule.isBlank()) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Name, dose and schedule are required."
            return
        }
        medicationPlanActionState = OneMedicationPlanActionState.SUBMITTING
        medicationPlanActionError = null
        try {
            lastMedicationPlan = apiClient.updateMedicationPlan(
                authenticatedSession,
                plan.id,
                MedicationPlanUpdateRequest(
                    name = cleanName,
                    dose = cleanDose,
                    schedule = cleanSchedule,
                    instructions = cleanInstructions,
                    active = active,
                    version = plan.version
                )
            )
            medicationPlanActionState = OneMedicationPlanActionState.LOADED
            loadMedicationReminders(plan.subjectUserId)
            loadMedicationPlans(plan.subjectUserId)
        } catch (error: Exception) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = error.message ?: "Could not update the medication plan."
        }
    }

    suspend fun updateMedicationDose(dose: MedicationDose, status: DoseStatus) {
        val authenticatedSession = session
        val planId = dose.planId
        val scheduledFor = dose.scheduledFor
        val wireStatus = status.medicationCheckInValue()
        if (!backendMode || authenticatedSession == null || planId == null || scheduledFor == null || wireStatus == null) {
            medicationActionError = "This reminder is not linked to a backend check-in."
            return
        }

        val actionKey = dose.medicationActionKey()
        medicationActionKey = actionKey
        medicationActionError = null
        try {
            apiClient.markMedicationCheckIn(
                session = authenticatedSession,
                planId = planId,
                scheduledFor = scheduledFor,
                status = wireStatus
            )
            loadMedicationReminders()
        } catch (error: Exception) {
            medicationActionError = error.message ?: "Could not update this medication check-in."
        } finally {
            if (medicationActionKey == actionKey) medicationActionKey = null
        }
    }

    suspend fun loadClips() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            clips = null
            clipLoadState = OneClipLoadState.IDLE
            clipLoadError = null
            return
        }
        clipLoadState = OneClipLoadState.LOADING
        clipLoadError = null
        try {
            clips = clipRepository.load(authenticatedSession)
            clipLoadState = OneClipLoadState.LOADED
        } catch (error: Exception) {
            clipLoadState = OneClipLoadState.ERROR
            clipLoadError = error.message ?: "Could not load event clips."
        }
    }

    suspend fun submitAssistantCheckIn(transcript: String) {
        val cleanTranscript = transcript.trim()
        if (cleanTranscript.isBlank()) {
            assistantLoadState = OneAssistantLoadState.ERROR
            assistantLoadError = "Write a short check-in before sending it."
            return
        }

        assistantLoadState = OneAssistantLoadState.SUBMITTING
        assistantLoadError = null
        try {
            val authenticatedSession = session
            assistantResult = if (backendMode && authenticatedSession != null) {
                apiClient.submitCheckIn(authenticatedSession, cleanTranscript)
            } else {
                OneCheckInResult(
                    id = null,
                    status = "unknown",
                    trend = "unknown",
                    explanation = "This demo check-in was kept on the device; connect a backend to receive a household summary.",
                    evidenceIds = emptyList(),
                    limitations = "Demo mode only. This is not medical advice.",
                    degraded = true
                )
            }
            assistantLoadState = OneAssistantLoadState.LOADED
        } catch (error: Exception) {
            assistantLoadState = OneAssistantLoadState.ERROR
            assistantLoadError = error.message ?: "ONE could not complete the check-in."
        }
    }

    suspend fun submitFamilyAssistant(message: String) {
        val authenticatedSession = session
        val subjectUserId = selectedFamilySubjectId
        if (!backendMode || authenticatedSession == null) {
            familyAssistantLoadState = OneFamilyAssistantLoadState.ERROR
            familyAssistantLoadError = "Connect a backend session before using the family assistant."
            return
        }
        if (!canManageFamily) {
            familyAssistantLoadState = OneFamilyAssistantLoadState.ERROR
            familyAssistantLoadError = "Only caregivers can use the family assistant."
            return
        }
        if (subjectUserId == null) {
            familyAssistantLoadState = OneFamilyAssistantLoadState.ERROR
            familyAssistantLoadError = "Select a person before asking the family assistant."
            return
        }
        familyAssistantLoadState = OneFamilyAssistantLoadState.SUBMITTING
        familyAssistantLoadError = null
        try {
            familyAssistantResult = apiClient.familyAssistant(
                session = authenticatedSession,
                message = message.trim().ifBlank { "Provide a concise administrative summary." },
                subjectUserId = subjectUserId
            )
            familyAssistantLoadState = OneFamilyAssistantLoadState.LOADED
        } catch (error: Exception) {
            familyAssistantLoadState = OneFamilyAssistantLoadState.ERROR
            familyAssistantLoadError = error.message ?: "Could not prepare the family assistant summary."
        }
    }

    suspend fun loadConsents() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            consentStates = null
            consentLoadState = OneConsentLoadState.IDLE
            consentLoadError = null
            return
        }
        consentLoadState = OneConsentLoadState.LOADING
        consentLoadError = null
        try {
            val latestByPurpose = linkedMapOf<String, Boolean>()
            apiClient.homeConsents(authenticatedSession).forEach { consent ->
                if (!latestByPurpose.containsKey(consent.purpose)) {
                    latestByPurpose[consent.purpose] = consent.revokedAt == null
                }
            }
            consentStates = latestByPurpose
            consentLoadState = OneConsentLoadState.LOADED
        } catch (error: Exception) {
            consentLoadState = OneConsentLoadState.ERROR
            consentLoadError = error.message ?: "Could not load privacy settings."
        }
    }

    suspend fun updateConsent(purpose: String, granted: Boolean) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) return
        consentUpdatePurpose = purpose
        consentUpdateError = null
        try {
            apiClient.recordConsent(
                session = authenticatedSession,
                consentRequest = ConsentRequest(
                    purpose = purpose,
                    policyVersion = "2026-09",
                    granted = granted
                )
            )
            consentStates = (consentStates ?: emptyMap()) + (purpose to granted)
            consentLoadState = OneConsentLoadState.LOADED
        } catch (error: Exception) {
            consentUpdateError = error.message ?: "Could not update this privacy setting."
        } finally {
            if (consentUpdatePurpose == purpose) consentUpdatePurpose = null
        }
    }

    suspend fun requestDataExport() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            exportLoadState = OneExportLoadState.ERROR
            exportLoadError = "Connect a backend session before preparing an export."
            return
        }
        exportLoadState = OneExportLoadState.SUBMITTING
        exportLoadError = null
        try {
            dataExport = apiClient.requestDataExport(authenticatedSession)
            exportLoadState = OneExportLoadState.LOADED
        } catch (error: Exception) {
            exportLoadState = OneExportLoadState.ERROR
            exportLoadError = error.message ?: "Could not prepare the data export."
        }
    }

    suspend fun requestDataDeletion() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            deletionLoadState = OneDeletionLoadState.ERROR
            deletionLoadError = "Connect a backend session before requesting deletion."
            return
        }
        if (!isAdmin) {
            deletionLoadState = OneDeletionLoadState.ERROR
            deletionLoadError = "Only the household administrator can request deletion."
            return
        }
        deletionLoadState = OneDeletionLoadState.SUBMITTING
        deletionLoadError = null
        try {
            dataDeletion = apiClient.requestDataDeletion(authenticatedSession)
            deletionLoadState = OneDeletionLoadState.LOADED
        } catch (error: Exception) {
            deletionLoadState = OneDeletionLoadState.ERROR
            deletionLoadError = error.message ?: "Could not request household deletion."
        }
    }

    suspend fun signOut() {
        val activeSession = session
        if (backendMode && activeSession != null) {
            runCatching { apiClient.logout(activeSession) }
        }
        withContext(Dispatchers.IO) { runCatching { secureStore.clear() } }
        session = null
        backendMode = false
        backendHealth = null
        backendHealthLoadState = OneBackendHealthLoadState.IDLE
        backendHealthError = null
        homeSnapshot = null
        homeLoadState = OneHomeLoadState.IDLE
        homeLoadError = null
        rooms = null
        currentRoomMap = null
        mapLoadState = OneMapLoadState.IDLE
        mapLoadError = null
        eventStreamState = OneEventStreamState.IDLE
        eventStreamError = null
        cameras = null
        cameraLoadState = OneCameraLoadState.IDLE
        cameraLoadError = null
        cameraActionState = OneCameraActionState.IDLE
        lastRegisteredCamera = null
        cameraActionError = null
        familyMembers = null
        familyLoadState = OneFamilyLoadState.IDLE
        familyLoadError = null
        familyInviteLoadState = OneFamilyInviteLoadState.IDLE
        familyInvite = null
        familyInviteLoadError = null
        selectedFamilySubjectId = null
        familySubjectInitialized = false
        medicationDoses = null
        medicationLoadState = OneMedicationLoadState.IDLE
        medicationLoadError = null
        medicationPlans = null
        medicationPlansLoadState = OneMedicationLoadState.IDLE
        medicationPlansLoadError = null
        medicationPlanActionState = OneMedicationPlanActionState.IDLE
        lastMedicationPlan = null
        medicationPlanActionError = null
        medicationActionKey = null
        medicationActionError = null
        assistantLoadState = OneAssistantLoadState.IDLE
        assistantResult = null
        assistantLoadError = null
        familyAssistantLoadState = OneFamilyAssistantLoadState.IDLE
        familyAssistantResult = null
        familyAssistantLoadError = null
        consentStates = null
        consentLoadState = OneConsentLoadState.IDLE
        consentLoadError = null
        consentUpdatePurpose = null
        consentUpdateError = null
        exportLoadState = OneExportLoadState.IDLE
        dataExport = null
        exportLoadError = null
        deletionLoadState = OneDeletionLoadState.IDLE
        dataDeletion = null
        deletionLoadError = null
        clips = null
        clipLoadState = OneClipLoadState.IDLE
        clipLoadError = null
        authStageName = AuthStage.SIGNED_OUT.name
    }
}

private fun DoseStatus.medicationCheckInValue(): String? = when (this) {
    DoseStatus.PENDING, DoseStatus.ACKNOWLEDGED, DoseStatus.NEEDS_CONFIRMATION, DoseStatus.SCHEDULED -> "pending"
    DoseStatus.TAKEN -> "taken"
    DoseStatus.SKIPPED -> "skipped"
    DoseStatus.MISSED -> "missed"
}

private fun MedicationDose.medicationActionKey(): String = "${planId}:${scheduledFor}"
