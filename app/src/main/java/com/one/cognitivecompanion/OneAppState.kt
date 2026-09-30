package com.one.cognitivecompanion

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

enum class OneCareAnalyticsLoadState { IDLE, LOADING, LOADED, ERROR }
/**
 * Compose-observable state and session actions for the top-level ONE flow.
 * Screens remain focused on rendering while this object owns persistence and
 * backend side effects.
 */
@Stable
class OneAppState(
    private val appContext: Context,
    val apiClient: OneApiClient,
    private val secureStore: OneSecureStore,
    private val homeRepository: OneHomeRepository,
    private val cameraRepository: OneCameraRepository,
    private val familyRepository: OneFamilyRepository,
    private val medicationRepository: OneMedicationRepository,
    private val clipRepository: OneClipRepository,
    private val offlineCache: OneOfflineCache
) {
    var authStageName by mutableStateOf(AuthStage.AUTHENTICATED.name)
    var roleName by mutableStateOf(OneRole.CAREGIVER.name)
    var selectedTab by mutableStateOf("home")
    var onboardingStep by mutableIntStateOf(0)
    var onboardingConsentRoom by mutableStateOf(false)
    var onboardingConsentMic by mutableStateOf(false)
    var onboardingConsentMedication by mutableStateOf(false)
    var onboardingConsentFamily by mutableStateOf(false)
    var onboardingConsentFamilyAssistant by mutableStateOf(false)
    var backendMode by mutableStateOf(false)
    var backendHealth by mutableStateOf<BackendHealth?>(null)
    var backendHealthLoadState by mutableStateOf(OneBackendHealthLoadState.IDLE)
    var backendHealthError by mutableStateOf<String?>(null)
    var session by mutableStateOf<OneSession?>(null)
    var homeSnapshot by mutableStateOf<OneHomeSnapshot?>(null)
    var homeLoadState by mutableStateOf(OneHomeLoadState.IDLE)
    var homeLoadError by mutableStateOf<String?>(null)
    var homeIsStale by mutableStateOf(false)
    var rooms by mutableStateOf<List<OneRoom>?>(null)
    var objectActionState by mutableStateOf(OneObjectActionState.IDLE)
    var objectActionError by mutableStateOf<String?>(null)
    var lastCreatedObject by mutableStateOf<OneRemoteObject?>(null)
    var observationActionState by mutableStateOf(OneObservationActionState.IDLE)
    var observationActionError by mutableStateOf<String?>(null)
    var lastObservation by mutableStateOf<OneObservationResult?>(null)
    var eventStreamState by mutableStateOf(OneEventStreamState.IDLE)
    var eventStreamError by mutableStateOf<String?>(null)
    var cameras by mutableStateOf<List<OneCamera>?>(null)
    var cameraLoadState by mutableStateOf(OneCameraLoadState.IDLE)
    var cameraLoadError by mutableStateOf<String?>(null)
    var camerasAreStale by mutableStateOf(false)
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
    var selectedCareRecipientId by mutableStateOf<UUID?>(null)
    var careRecipientSelectionInitialized by mutableStateOf(false)
    var medicationDoses by mutableStateOf<List<MedicationDose>?>(null)
    var medicationLoadState by mutableStateOf(OneMedicationLoadState.IDLE)
    var medicationLoadError by mutableStateOf<String?>(null)
    var medicationPlans by mutableStateOf<List<OneMedicationPlan>?>(null)
    var medicationPlansLoadState by mutableStateOf(OneMedicationLoadState.IDLE)
    var medicationPlansLoadError by mutableStateOf<String?>(null)
    var medicationCheckIns by mutableStateOf<List<OneRemoteMedicationCheckIn>?>(null)
    var medicationCheckInsLoadState by mutableStateOf(OneMedicationLoadState.IDLE)
    var medicationCheckInsLoadError by mutableStateOf<String?>(null)
    var medicationHistoryDays by mutableIntStateOf(7)
    var medicationPlanActionState by mutableStateOf(OneMedicationPlanActionState.IDLE)
    var lastMedicationPlan by mutableStateOf<OneMedicationPlan?>(null)
    var medicationPlanActionError by mutableStateOf<String?>(null)
    var careEntries by mutableStateOf<List<OneCareEntry>?>(null)
    var careEntriesError by mutableStateOf<String?>(null)
    var careEntryActionError by mutableStateOf<String?>(null)
    var careEntrySaving by mutableStateOf(false)
    var medicationActionKey by mutableStateOf<String?>(null)
    var medicationActionError by mutableStateOf<String?>(null)
    var assistantLoadState by mutableStateOf(OneAssistantLoadState.IDLE)
    var assistantResult by mutableStateOf<OneCheckInResult?>(null)
    var assistantLoadError by mutableStateOf<String?>(null)
    var familyAssistantLoadState by mutableStateOf(OneFamilyAssistantLoadState.IDLE)
    var familyAssistantResult by mutableStateOf<OneFamilyAssistantResult?>(null)
    var familyAssistantLoadError by mutableStateOf<String?>(null)
    var consentStates by mutableStateOf<Map<String, Boolean>?>(null)
    /** Latest consent state keyed by the represented subject, never mixed across people. */
    var consentStatesBySubject by mutableStateOf<Map<UUID, Map<String, Boolean>>>(emptyMap())
    var consentStatesByCareRecipient by mutableStateOf<Map<UUID, Map<String, Boolean>>>(emptyMap())
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
    var publisherPairing by mutableStateOf<PublisherPairingStartResponse?>(null)
    var publisherPairingLoadState by mutableStateOf(OneFamilyInviteLoadState.IDLE)
    var publisherPairingError by mutableStateOf<String?>(null)
    var cameraPairingStatus by mutableStateOf<OneCameraPairingStatus?>(null)
    var cameraPairingStatusLoadState by mutableStateOf(OneCameraPairingStatusLoadState.IDLE)
    var cameraPairingStatusError by mutableStateOf<String?>(null)
    var cameraReconnectLink by mutableStateOf<OneCameraReconnectLink?>(null)
    var cameraReconnectLoadState by mutableStateOf(OneCameraReconnectLoadState.IDLE)
    var cameraReconnectError by mutableStateOf<String?>(null)
    var careSpaces by mutableStateOf<List<OneCareSpace>?>(null)
    var careSpacesLoadState by mutableStateOf(OneCareSpaceLoadState.IDLE)
    var careSpacesLoadError by mutableStateOf<String?>(null)
    var careSpaceActionState by mutableStateOf(OneCareSpaceActionState.IDLE)
    var careSpaceActionError by mutableStateOf<String?>(null)
    var careRecipients by mutableStateOf<List<OneCareRecipient>?>(null)
    var careRecipientsLoadState by mutableStateOf(OneCareRecipientLoadState.IDLE)
    var careRecipientsLoadError by mutableStateOf<String?>(null)
    var careRecipientActionState by mutableStateOf(OneCareRecipientActionState.IDLE)
    var careRecipientActionError by mutableStateOf<String?>(null)
    var careAnalytics by mutableStateOf<OneCareAnalytics?>(null)
    var careAnalyticsLoadState by mutableStateOf(OneCareAnalyticsLoadState.IDLE)
    var careAnalyticsLoadError by mutableStateOf<String?>(null)
    var familyMemberActionState by mutableStateOf(OneFamilyMemberActionState.IDLE)
    var familyMemberActionError by mutableStateOf<String?>(null)
    var familyMemberActionId by mutableStateOf<UUID?>(null)

    val isAdmin: Boolean
        get() = session?.backendRole?.equals("admin", ignoreCase = true) == true

    val canManageFamily: Boolean
        get() = backendMode && session?.backendRole?.let { role ->
            role.equals("admin", ignoreCase = true) || role.equals("caregiver", ignoreCase = true)
        } == true

    private fun consentIsKnownAndDenied(purpose: String, subjectUserId: UUID?): Boolean =
        backendMode && subjectUserId != null && consentStatesBySubject.containsKey(subjectUserId) &&
            consentStatesBySubject[subjectUserId]?.get(purpose) != true

    private fun recipientConsentIsKnownAndDenied(purpose: String, careRecipientId: UUID?): Boolean =
        backendMode && careRecipientId != null && consentStatesByCareRecipient.containsKey(careRecipientId) &&
            consentStatesByCareRecipient[careRecipientId]?.get(purpose) != true

    suspend fun restoreSession() {
        val reconnectCandidate = withContext(Dispatchers.IO) { secureStore.restorePublisherForReconnect() }
        val stored = withContext(Dispatchers.IO) { secureStore.restore() } ?: reconnectCandidate ?: return
        val restored = if (
            reconnectCandidate != null &&
            reconnectCandidate.session.expiresAt?.isBefore(Instant.now()) == true
        ) {
            val refreshed = runCatching {
                apiClient.reconnectCamera(
                    reconnectCandidate.session.userId,
                    reconnectCandidate.session.reconnectToken.orEmpty()
                ).copy(reconnectToken = reconnectCandidate.session.reconnectToken)
            }.getOrNull() ?: return
            withContext(Dispatchers.IO) { secureStore.saveSession(refreshed, onboardingComplete = true) }
            StoredOneSession(refreshed, true)
        } else {
            stored
        }
        session = restored.session
        backendMode = true
        roleName = restored.session.role.name
        selectedTab = when (restored.session.role) {
            OneRole.RESIDENT -> "today"
            OneRole.PUBLISHER -> "publisher"
            OneRole.CAREGIVER -> "home"
        }
        if (restored.onboardingComplete || restored.session.role == OneRole.PUBLISHER) {
            onboardingStep = 3
            authStageName = AuthStage.AUTHENTICATED.name
        } else {
            onboardingConsentRoom = false
            onboardingConsentMic = false
            onboardingConsentMedication = false
            onboardingConsentFamily = false
            onboardingConsentFamilyAssistant = false
            onboardingStep = 0
            authStageName = AuthStage.ONBOARDING.name
        }
    }

    fun applyAuthenticatedSession(authenticatedSession: OneSession?, usedBackend: Boolean) {
        if (authenticatedSession != null && usedBackend) OneCareAppointmentScheduler.bindSession(appContext, authenticatedSession)
        else if (session != null) OneCareAppointmentScheduler.clear(appContext)
        session = authenticatedSession
        backendMode = usedBackend
        backendHealth = null
        backendHealthLoadState = OneBackendHealthLoadState.IDLE
        backendHealthError = null
        homeSnapshot = null
        homeLoadState = OneHomeLoadState.IDLE
        homeLoadError = null
        homeIsStale = false
        rooms = null
        objectActionState = OneObjectActionState.IDLE
        objectActionError = null
        lastCreatedObject = null
        observationActionState = OneObservationActionState.IDLE
        observationActionError = null
        lastObservation = null
        eventStreamState = OneEventStreamState.IDLE
        eventStreamError = null
        cameras = null
        cameraLoadState = OneCameraLoadState.IDLE
        cameraLoadError = null
        camerasAreStale = false
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
        selectedCareRecipientId = null
        careRecipientSelectionInitialized = false
        careEntries = null
        careEntriesError = null
        careEntryActionError = null
        medicationDoses = null
        medicationLoadState = OneMedicationLoadState.IDLE
        medicationLoadError = null
        medicationPlans = null
        medicationPlansLoadState = OneMedicationLoadState.IDLE
        medicationPlansLoadError = null
        medicationCheckIns = null
        medicationCheckInsLoadState = OneMedicationLoadState.IDLE
        medicationCheckInsLoadError = null
        medicationHistoryDays = 7
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
        consentStatesBySubject = emptyMap()
        consentStatesByCareRecipient = emptyMap()
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
        publisherPairing = null
        publisherPairingLoadState = OneFamilyInviteLoadState.IDLE
        publisherPairingError = null
        cameraPairingStatus = null
        cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.IDLE
        cameraPairingStatusError = null
        cameraReconnectLink = null
        cameraReconnectLoadState = OneCameraReconnectLoadState.IDLE
        cameraReconnectError = null
        careSpaces = null
        careSpacesLoadState = OneCareSpaceLoadState.IDLE
        careSpacesLoadError = null
        careSpaceActionState = OneCareSpaceActionState.IDLE
        careSpaceActionError = null
        careRecipients = null
        careRecipientsLoadState = OneCareRecipientLoadState.IDLE
        careRecipientsLoadError = null
        careRecipientActionState = OneCareRecipientActionState.IDLE
        careRecipientActionError = null
        familyMemberActionState = OneFamilyMemberActionState.IDLE
        familyMemberActionError = null
        familyMemberActionId = null
        onboardingConsentRoom = false
        onboardingConsentMic = false
        onboardingConsentMedication = false
        onboardingConsentFamily = false
        onboardingConsentFamilyAssistant = false
        val onboardingComplete = if (usedBackend && authenticatedSession != null) {
            runCatching {
                secureStore.saveSession(authenticatedSession, onboardingComplete = false)
                secureStore.isOnboardingComplete(authenticatedSession)
            }.getOrDefault(false)
        } else {
            false
        }
        roleName = authenticatedSession?.role?.name ?: OneRole.CAREGIVER.name
        onboardingStep = if (authenticatedSession?.role == OneRole.PUBLISHER || onboardingComplete) 3 else 0
        authStageName = if (authenticatedSession?.role == OneRole.PUBLISHER || onboardingComplete) {
            AuthStage.AUTHENTICATED.name
        } else {
            AuthStage.ONBOARDING.name
        }
        selectedTab = when (authenticatedSession?.role) {
            OneRole.RESIDENT -> "today"
            OneRole.PUBLISHER -> "publisher"
            else -> "home"
        }
    }

    suspend fun recordOnboardingConsents() {
        val authenticatedSession = session ?: return
        if (!backendMode) return
        val choices = listOf(
            "audio_capture" to onboardingConsentMic,
            "video_capture" to onboardingConsentRoom,
            "medication_management" to onboardingConsentMedication,
            "family_mode" to onboardingConsentFamily,
            "family_assistant" to onboardingConsentFamilyAssistant
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
        selectedTab = when (roleName) {
            OneRole.RESIDENT.name -> "today"
            OneRole.PUBLISHER.name -> "publisher"
            else -> "home"
        }
    }

    suspend fun createPublisherPairing(label: String) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            publisherPairingLoadState = OneFamilyInviteLoadState.ERROR
            publisherPairingError = "Connect a caregiver backend session before pairing a publisher device."
            return
        }
        if (!canManageFamily) {
            publisherPairingLoadState = OneFamilyInviteLoadState.ERROR
            publisherPairingError = "Only a caregiver or administrator can pair a publisher device."
            return
        }
        val cleanLabel = label.trim()
        if (cleanLabel.isBlank()) {
            publisherPairingLoadState = OneFamilyInviteLoadState.ERROR
            publisherPairingError = "Enter a label for the publisher device."
            return
        }
        publisherPairingLoadState = OneFamilyInviteLoadState.SUBMITTING
        publisherPairingError = null
        cameraPairingStatus = null
        cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.IDLE
        cameraPairingStatusError = null
        try {
            publisherPairing = apiClient.startPublisherPairing(
                authenticatedSession,
                PublisherPairingStartRequest(cleanLabel)
            )
            publisherPairingLoadState = OneFamilyInviteLoadState.LOADED
            refreshCameraPairingStatus(publisherPairing?.pairingId)
        } catch (error: Exception) {
            publisherPairingLoadState = OneFamilyInviteLoadState.ERROR
            publisherPairingError = error.message ?: "Could not create the publisher pairing code."
        }
    }

    suspend fun refreshCameraPairingStatus(pairingId: UUID? = publisherPairing?.pairingId) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || pairingId == null) {
            cameraPairingStatus = null
            cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.IDLE
            cameraPairingStatusError = null
            return
        }
        if (!canManageFamily) {
            cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.ERROR
            cameraPairingStatusError = "Only a caregiver or administrator can view pairing status."
            return
        }
        cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.LOADING
        cameraPairingStatusError = null
        try {
            cameraPairingStatus = apiClient.cameraPairingStatus(authenticatedSession, pairingId)
            cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.LOADED
            if (cameraPairingStatus?.status == "connected") loadCameras()
        } catch (error: Exception) {
            cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.ERROR
            cameraPairingStatusError = error.message ?: "Could not load the camera pairing status."
        }
    }

    suspend fun createCameraReconnectLink() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || authenticatedSession.role != OneRole.PUBLISHER) {
            cameraReconnectLoadState = OneCameraReconnectLoadState.ERROR
            cameraReconnectError = "Only a paired publisher device can create a reconnect link."
            return
        }
        cameraReconnectLoadState = OneCameraReconnectLoadState.SUBMITTING
        cameraReconnectError = null
        try {
            cameraReconnectLink = apiClient.createCameraReconnectLink(authenticatedSession)
            session = authenticatedSession.copy(reconnectToken = cameraReconnectLink?.reconnectToken)
            withContext(Dispatchers.IO) { secureStore.saveSession(session!!, onboardingComplete = true) }
            cameraReconnectLoadState = OneCameraReconnectLoadState.LOADED
        } catch (error: Exception) {
            cameraReconnectLoadState = OneCameraReconnectLoadState.ERROR
            cameraReconnectError = error.message ?: "Could not create the camera reconnect link."
        }
    }

    suspend fun reconnectPublisherCamera() {
        val authenticatedSession = session
        val reconnectToken = authenticatedSession?.reconnectToken
        if (!backendMode || authenticatedSession == null || authenticatedSession.role != OneRole.PUBLISHER || reconnectToken.isNullOrBlank()) {
            cameraReconnectLoadState = OneCameraReconnectLoadState.ERROR
            cameraReconnectError = "This publisher has no stored reconnect link yet."
            return
        }
        cameraReconnectLoadState = OneCameraReconnectLoadState.SUBMITTING
        cameraReconnectError = null
        try {
            val reconnected = apiClient.reconnectCamera(authenticatedSession.userId, reconnectToken)
            session = reconnected.copy(reconnectToken = reconnectToken)
            withContext(Dispatchers.IO) { secureStore.saveSession(session!!, onboardingComplete = true) }
            cameraReconnectLoadState = OneCameraReconnectLoadState.LOADED
        } catch (error: Exception) {
            cameraReconnectLoadState = OneCameraReconnectLoadState.ERROR
            cameraReconnectError = error.message ?: "Could not reconnect the publisher camera."
        }
    }

    suspend fun loadCareSpaces() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || authenticatedSession.role == OneRole.PUBLISHER) {
            careSpaces = null
            careSpacesLoadState = OneCareSpaceLoadState.IDLE
            careSpacesLoadError = null
            return
        }
        careSpacesLoadState = OneCareSpaceLoadState.LOADING
        careSpacesLoadError = null
        try {
            careSpaces = apiClient.careSpaces(authenticatedSession)
            careSpacesLoadState = OneCareSpaceLoadState.LOADED
        } catch (error: Exception) {
            careSpacesLoadState = OneCareSpaceLoadState.ERROR
            careSpacesLoadError = error.message ?: "Could not load your care spaces."
        }
    }

    suspend fun createCareSpace(name: String, careSetting: String, supportFocus: String) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            careSpaceActionState = OneCareSpaceActionState.ERROR
            careSpaceActionError = "Connect a backend session before creating a care space."
            return
        }
        if (!canManageFamily) {
            careSpaceActionState = OneCareSpaceActionState.ERROR
            careSpaceActionError = "Only caregivers or administrators can create a care space."
            return
        }
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            careSpaceActionState = OneCareSpaceActionState.ERROR
            careSpaceActionError = "Enter a name for the care space."
            return
        }
        careSpaceActionState = OneCareSpaceActionState.SUBMITTING
        careSpaceActionError = null
        try {
            val newSession = apiClient.createCareSpace(
                authenticatedSession,
                CareSpaceCreateRequest(cleanName, careSetting, supportFocus)
            )
            applyAuthenticatedSession(newSession, usedBackend = true)
            careSpaceActionState = OneCareSpaceActionState.LOADED
            loadCareSpaces()
        } catch (error: Exception) {
            careSpaceActionState = OneCareSpaceActionState.ERROR
            careSpaceActionError = error.message ?: "Could not create the care space."
        }
    }

    suspend fun activateCareSpace(space: OneCareSpace) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || authenticatedSession.homeId == space.id) return
        careSpaceActionState = OneCareSpaceActionState.SUBMITTING
        careSpaceActionError = null
        try {
            val newSession = apiClient.activateCareSpace(authenticatedSession, space.id)
            applyAuthenticatedSession(newSession, usedBackend = true)
            careSpaceActionState = OneCareSpaceActionState.LOADED
            loadCareSpaces()
        } catch (error: Exception) {
            careSpaceActionState = OneCareSpaceActionState.ERROR
            careSpaceActionError = error.message ?: "Could not switch care space."
        }
    }

    suspend fun loadCareRecipients() {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || authenticatedSession.role == OneRole.PUBLISHER) {
            careRecipients = null
            careRecipientsLoadState = OneCareRecipientLoadState.IDLE
            careRecipientsLoadError = null
            return
        }
        careRecipientsLoadState = OneCareRecipientLoadState.LOADING
        careRecipientsLoadError = null
        try {
            careRecipients = apiClient.careRecipients(authenticatedSession)
            if (!careRecipientSelectionInitialized || careRecipients.orEmpty().none { it.id == selectedCareRecipientId }) {
                selectedCareRecipientId = careRecipients.orEmpty().firstOrNull()?.id
                careRecipientSelectionInitialized = true
            }
            careRecipientsLoadState = OneCareRecipientLoadState.LOADED
            loadCareAnalytics()
            loadCareEntries()
            if (consentLoadState == OneConsentLoadState.LOADED) {
                careRecipients.orEmpty().filter { consentStatesByCareRecipient[it.id]?.get("care_planning") != true }.forEach { recipient ->
                    OneCareAppointmentScheduler.sync(appContext, recipient.id, emptyList())
                }
            }
            careRecipients.orEmpty().filter { it.id != selectedCareRecipientId && consentStatesByCareRecipient[it.id]?.get("care_planning") == true }.forEach { recipient ->
                runCatching { apiClient.careEntries(authenticatedSession, recipient.id) }
                    .onSuccess { OneCareAppointmentScheduler.sync(appContext, recipient.id, it) }
            }
        } catch (error: Exception) {
            careRecipientsLoadState = OneCareRecipientLoadState.ERROR
            careRecipientsLoadError = error.message ?: "Could not load care recipients."
        }
    }

    suspend fun createCareRecipient(displayName: String, relationship: String?, roomLabel: String?) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Connect a backend session before adding a care recipient."
            return
        }
        if (!canManageFamily) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Only caregivers can manage care recipients."
            return
        }
        if (displayName.trim().isBlank()) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Enter the person's name."
            return
        }
        careRecipientActionState = OneCareRecipientActionState.SUBMITTING
        careRecipientActionError = null
        try {
            val created = apiClient.createCareRecipient(
                authenticatedSession,
                CareRecipientCreateRequest(displayName, relationship, roomLabel)
            )
            careRecipients = (careRecipients.orEmpty().filterNot { it.id == created.id } + created).sortedBy { it.displayName.lowercase() }
            if (selectedCareRecipientId == null) selectedCareRecipientId = created.id
            careRecipientActionState = OneCareRecipientActionState.LOADED
        } catch (error: Exception) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = error.message ?: "Could not add the care recipient."
        }
    }

    suspend fun updateCareRecipient(recipient: OneCareRecipient, displayName: String, relationship: String?, roomLabel: String?) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || !canManageFamily) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Only caregivers can update care recipients."
            return
        }
        if (displayName.trim().isBlank()) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Enter the person's name."
            return
        }
        careRecipientActionState = OneCareRecipientActionState.SUBMITTING
        careRecipientActionError = null
        try {
            val updated = apiClient.updateCareRecipient(
                authenticatedSession,
                recipient.id,
                CareRecipientUpdateRequest(displayName, relationship, roomLabel)
            )
            careRecipients = careRecipients.orEmpty().map { if (it.id == updated.id) updated else it }
            careRecipientActionState = OneCareRecipientActionState.LOADED
        } catch (error: Exception) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = error.message ?: "Could not update the care recipient."
        }
    }

    suspend fun deleteCareRecipient(recipient: OneCareRecipient) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || !canManageFamily) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Only caregivers can remove care recipients."
            return
        }
        careRecipientActionState = OneCareRecipientActionState.SUBMITTING
        careRecipientActionError = null
        try {
            apiClient.deleteCareRecipient(authenticatedSession, recipient.id)
            careRecipients = careRecipients.orEmpty().filterNot { it.id == recipient.id }
            if (selectedCareRecipientId == recipient.id) selectedCareRecipientId = careRecipients.orEmpty().firstOrNull()?.id
            careRecipientActionState = OneCareRecipientActionState.LOADED
        } catch (error: Exception) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = error.message ?: "Could not remove the care recipient."
        }
    }

    suspend fun enrollFaceProfile(recipient: OneCareRecipient, frames: List<OneFaceEnrollmentFrame>) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || !canManageFamily) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Only caregivers connected to ONE can set up face recognition."
            return
        }
        if (frames.size < 3) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Capture a front, right and left view before continuing."
            return
        }
        careRecipientActionState = OneCareRecipientActionState.SUBMITTING
        careRecipientActionError = null
        try {
            apiClient.recordConsent(
                authenticatedSession,
                ConsentRequest(
                    purpose = "face_recognition",
                    policyVersion = "2026-09",
                    granted = true,
                    careRecipientId = recipient.id
                )
            )
            val profile = apiClient.enrollFaceProfile(authenticatedSession, recipient.id, frames)
            careRecipients = careRecipients.orEmpty().map { item ->
                if (item.id == recipient.id) item.copy(
                    faceRecognitionStatus = profile.status,
                    faceProfileUpdatedAt = profile.updatedAt
                ) else item
            }
            careRecipientActionState = OneCareRecipientActionState.LOADED
        } catch (error: Exception) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = error.message ?: "Could not set up face recognition for this person."
        }
    }

    suspend fun disableFaceProfile(recipient: OneCareRecipient) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || !canManageFamily) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = "Only caregivers connected to ONE can disable face recognition."
            return
        }
        careRecipientActionState = OneCareRecipientActionState.SUBMITTING
        careRecipientActionError = null
        try {
            val profile = apiClient.deleteFaceProfile(authenticatedSession, recipient.id)
            careRecipients = careRecipients.orEmpty().map { item ->
                if (item.id == recipient.id) item.copy(
                    faceRecognitionStatus = profile.status,
                    faceProfileUpdatedAt = profile.updatedAt
                ) else item
            }
            careRecipientActionState = OneCareRecipientActionState.LOADED
        } catch (error: Exception) {
            careRecipientActionState = OneCareRecipientActionState.ERROR
            careRecipientActionError = error.message ?: "Could not disable face recognition for this person."
        }
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
            homeSnapshot?.let { offlineCache.saveHome(authenticatedSession.homeId, it) }
            homeIsStale = false
            homeLoadState = OneHomeLoadState.LOADED
        } catch (error: Exception) {
            homeSnapshot = offlineCache.readHome(authenticatedSession.homeId)
            homeIsStale = homeSnapshot != null
            homeLoadState = if (homeSnapshot == null) OneHomeLoadState.ERROR else OneHomeLoadState.LOADED
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
            cameras?.let { offlineCache.saveCameras(authenticatedSession.homeId, it) }
            camerasAreStale = false
            runCatching { apiClient.homeRooms(authenticatedSession) }
                .onSuccess { rooms = it }
            cameraLoadState = OneCameraLoadState.LOADED
        } catch (error: Exception) {
            cameras = offlineCache.readCameras(authenticatedSession.homeId).takeIf { it.isNotEmpty() }
            camerasAreStale = cameras != null
            cameraLoadState = if (cameras == null) OneCameraLoadState.ERROR else OneCameraLoadState.LOADED
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

    suspend fun updateCamera(camera: OneCamera, name: String, roomId: UUID?, enabled: Boolean) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Connect a backend session before updating a camera."
            return
        }
        if (!canManageFamily) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Only caregivers can update household cameras."
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
            val remote = apiClient.updateCamera(
                authenticatedSession,
                camera.id,
                CameraUpdateRequest(name = cleanName, roomId = roomId, enabled = enabled)
            )
            val updated = OneCamera(
                id = remote.id,
                name = remote.name,
                roomId = remote.roomId,
                platform = remote.platform,
                status = remote.status,
                enabled = remote.enabled,
                lastSeenAt = remote.lastSeenAt
            )
            cameras = cameras.orEmpty().map { if (it.id == updated.id) updated else it }
            lastRegisteredCamera = updated
            cameraActionState = OneCameraActionState.LOADED
        } catch (error: Exception) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = error.message ?: "Could not update the camera."
        }
    }

    suspend fun deleteCamera(camera: OneCamera) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || !canManageFamily) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = "Only caregivers can remove household cameras."
            return
        }
        cameraActionState = OneCameraActionState.SUBMITTING
        cameraActionError = null
        try {
            apiClient.deleteCamera(authenticatedSession, camera.id)
            cameras = cameras.orEmpty().filterNot { it.id == camera.id }
            offlineCache.saveCameras(authenticatedSession.homeId, cameras.orEmpty())
            if (lastRegisteredCamera?.id == camera.id) lastRegisteredCamera = null
            cameraActionState = OneCameraActionState.LOADED
        } catch (error: Exception) {
            cameraActionState = OneCameraActionState.ERROR
            cameraActionError = error.message ?: "Could not remove the camera."
        }
    }


    suspend fun createObject(label: String, displayName: String?) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            objectActionState = OneObjectActionState.ERROR
            objectActionError = "Connect a backend session before adding an object."
            return
        }
        if (!canManageFamily) {
            objectActionState = OneObjectActionState.ERROR
            objectActionError = "Only caregivers can add household objects."
            return
        }
        val cleanLabel = label.trim()
        if (cleanLabel.isBlank()) {
            objectActionState = OneObjectActionState.ERROR
            objectActionError = "Enter an object label."
            return
        }
        objectActionState = OneObjectActionState.SUBMITTING
        objectActionError = null
        try {
            lastCreatedObject = apiClient.createObject(authenticatedSession, OneObjectRequest(cleanLabel, displayName?.trim()?.takeIf { it.isNotBlank() }))
            objectActionState = OneObjectActionState.LOADED
            loadHome()
        } catch (error: Exception) {
            objectActionState = OneObjectActionState.ERROR
            objectActionError = error.message ?: "Could not add the object."
        }
    }

    suspend fun submitObservation(
        objectId: UUID?,
        cameraId: UUID?,
        mapId: UUID?,
        x: Double?,
        y: Double?,
        uncertaintyM: Double?,
        confidence: Double
    ) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            observationActionState = OneObservationActionState.ERROR
            observationActionError = "Connect a backend session before recording an observation."
            return
        }
        if (!canManageFamily) {
            observationActionState = OneObservationActionState.ERROR
            observationActionError = "Only caregivers can record household observations."
            return
        }
        if (objectId == null && cameraId == null) {
            observationActionState = OneObservationActionState.ERROR
            observationActionError = "Select an object or camera for this observation."
            return
        }
        observationActionState = OneObservationActionState.SUBMITTING
        observationActionError = null
        try {
            lastObservation = apiClient.submitObservation(
                authenticatedSession,
                OneObservationRequest(
                    objectId = objectId,
                    cameraId = cameraId,
                    mapId = mapId,
                    x = x,
                    y = y,
                    uncertaintyM = uncertaintyM,
                    confidence = confidence.coerceIn(0.0, 1.0)
                )
            )
            observationActionState = OneObservationActionState.LOADED
            loadHome()
        } catch (error: Exception) {
            observationActionState = OneObservationActionState.ERROR
            observationActionError = error.message ?: "Could not save the observation."
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
        loadMedicationCheckIns()
    }

    suspend fun selectCareRecipient(careRecipientId: UUID) {
        if (!backendMode || session == null || careRecipients.orEmpty().none { it.id == careRecipientId }) return
        selectedCareRecipientId = careRecipientId
        careRecipientSelectionInitialized = true
        medicationDoses = null
        medicationPlans = null
        medicationCheckIns = null
        careEntries = null
        medicationActionError = null
        assistantResult = null
        familyAssistantResult = null
        loadMedicationReminders()
        loadMedicationPlans()
        loadMedicationCheckIns()
        loadCareAnalytics()
        loadCareEntries()
    }

    suspend fun loadCareEntries(careRecipientId: UUID? = selectedCareRecipientId) {
        val authenticatedSession = session ?: return
        if (!backendMode || careRecipientId == null || consentStatesByCareRecipient[careRecipientId]?.get("care_planning") != true) {
            careEntries = null
            careEntriesError = null
            if (careRecipientId != null && consentLoadState == OneConsentLoadState.LOADED) OneCareAppointmentScheduler.sync(appContext, careRecipientId, emptyList())
            return
        }
        careEntriesError = null
        try {
            val result = apiClient.careEntries(authenticatedSession, careRecipientId)
            if (careRecipientId == selectedCareRecipientId) {
                careEntries = result
                OneCareAppointmentScheduler.sync(appContext, careRecipientId, result)
            }
        } catch (error: Exception) {
            careEntriesError = error.message ?: "Could not load notes and appointments."
        }
    }

    suspend fun saveCareEntry(request: OneCareEntryRequest, existing: OneCareEntry? = null): Boolean {
        val authenticatedSession = session ?: return false
        if (!backendMode || !canManageFamily || request.careRecipientId != selectedCareRecipientId) return false
        careEntrySaving = true
        careEntryActionError = null
        return try {
            val saved = if (existing == null) apiClient.createCareEntry(authenticatedSession, request)
            else apiClient.updateCareEntry(authenticatedSession, existing, request)
            careEntries = careEntries.orEmpty().filterNot { it.id == saved.id } + saved
            OneCareAppointmentScheduler.sync(appContext, request.careRecipientId, careEntries.orEmpty())
            loadCareEntries()
            true
        } catch (error: Exception) {
            careEntryActionError = error.message ?: "Could not save this entry."
            false
        } finally {
            careEntrySaving = false
        }
    }

    suspend fun removeCareEntry(entry: OneCareEntry): Boolean {
        val authenticatedSession = session ?: return false
        careEntrySaving = true
        careEntryActionError = null
        return try {
            apiClient.removeCareEntry(authenticatedSession, entry)
            careEntries = careEntries.orEmpty().filterNot { it.id == entry.id }
            OneCareAppointmentScheduler.sync(appContext, entry.careRecipientId, careEntries.orEmpty())
            loadCareEntries()
            true
        } catch (error: Exception) {
            careEntryActionError = error.message ?: "Could not remove this entry."
            false
        } finally {
            careEntrySaving = false
        }
    }

    suspend fun loadCareAnalytics(careRecipientId: UUID? = selectedCareRecipientId) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || careRecipientId == null) {
            careAnalytics = null
            careAnalyticsLoadState = OneCareAnalyticsLoadState.IDLE
            careAnalyticsLoadError = null
            return
        }
        careAnalyticsLoadState = OneCareAnalyticsLoadState.LOADING
        careAnalyticsLoadError = null
        try {
            careAnalytics = apiClient.careAnalytics(authenticatedSession, careRecipientId)
            careAnalyticsLoadState = OneCareAnalyticsLoadState.LOADED
        } catch (error: Exception) {
            careAnalytics = null
            careAnalyticsLoadState = OneCareAnalyticsLoadState.ERROR
            careAnalyticsLoadError = error.message ?: "Could not load the 30-day safety context."
        }
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

    suspend fun updateFamilyMember(member: OneFamilyMember, role: OneRole) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "Connect a backend session before changing family access."
            return
        }
        if (!canManageFamily || authenticatedSession.userId == member.id) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "You cannot change your own household access."
            return
        }
        if (member.role.equals("admin", ignoreCase = true)) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "Administrator access is managed separately."
            return
        }
        if (consentIsKnownAndDenied("family_mode", authenticatedSession.userId)) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "Active family_mode consent is required to manage access."
            return
        }
        familyMemberActionState = OneFamilyMemberActionState.SUBMITTING
        familyMemberActionId = member.id
        familyMemberActionError = null
        try {
            apiClient.updateFamilyMember(authenticatedSession, member.id, FamilyMemberUpdateRequest(role))
            familyMemberActionState = OneFamilyMemberActionState.LOADED
            loadFamily()
        } catch (error: Exception) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = error.message ?: "Could not update family access."
        } finally {
            if (familyMemberActionId == member.id) familyMemberActionId = null
        }
    }

    suspend fun removeFamilyMember(member: OneFamilyMember) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "Connect a backend session before removing family access."
            return
        }
        if (!canManageFamily || authenticatedSession.userId == member.id) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "You cannot remove your own household access."
            return
        }
        if (member.role.equals("admin", ignoreCase = true)) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = "Administrator access cannot be removed here."
            return
        }
        familyMemberActionState = OneFamilyMemberActionState.SUBMITTING
        familyMemberActionId = member.id
        familyMemberActionError = null
        try {
            apiClient.removeFamilyMember(authenticatedSession, member.id)
            familyMemberActionState = OneFamilyMemberActionState.LOADED
            loadFamily()
        } catch (error: Exception) {
            familyMemberActionState = OneFamilyMemberActionState.ERROR
            familyMemberActionError = error.message ?: "Could not remove family access."
        } finally {
            if (familyMemberActionId == member.id) familyMemberActionId = null
        }
    }

    suspend fun loadMedicationReminders(
        subjectUserId: UUID? = null,
        careRecipientId: UUID? = selectedCareRecipientId
    ) {
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
            medicationDoses = medicationRepository.load(authenticatedSession, subjectUserId, careRecipientId)
            runCatching {
                val reminders = apiClient.medicationReminders(authenticatedSession, subjectUserId = subjectUserId, careRecipientId = careRecipientId)
                OneMedicationScheduler.sync(appContext, reminders)
            }
            medicationLoadState = OneMedicationLoadState.LOADED
        } catch (error: Exception) {
            medicationLoadState = OneMedicationLoadState.ERROR
            medicationLoadError = error.message ?: "Could not load medication reminders."
        }
    }

    suspend fun loadMedicationPlans(
        subjectUserId: UUID? = null,
        careRecipientId: UUID? = selectedCareRecipientId
    ) {
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
            medicationPlans = apiClient.medicationPlans(authenticatedSession, subjectUserId = subjectUserId, careRecipientId = careRecipientId)
            medicationPlansLoadState = OneMedicationLoadState.LOADED
        } catch (error: Exception) {
            medicationPlansLoadState = OneMedicationLoadState.ERROR
            medicationPlansLoadError = error.message ?: "Could not load medication plans."
        }
    }

    suspend fun loadMedicationCheckIns(
        subjectUserId: UUID? = null,
        careRecipientId: UUID? = selectedCareRecipientId,
        days: Int = medicationHistoryDays
    ) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            medicationCheckIns = null
            medicationCheckInsLoadState = OneMedicationLoadState.IDLE
            medicationCheckInsLoadError = null
            return
        }
        val boundedDays = days.coerceIn(1, 30)
        medicationHistoryDays = boundedDays
        medicationCheckInsLoadState = OneMedicationLoadState.LOADING
        medicationCheckInsLoadError = null
        try {
            val now = Instant.now()
            medicationCheckIns = apiClient.medicationCheckIns(
                session = authenticatedSession,
                subjectUserId = subjectUserId,
                careRecipientId = careRecipientId,
                scheduledFrom = now.minusSeconds(boundedDays.toLong() * 86_400L),
                scheduledTo = now.plusSeconds(86_400L)
            )
            medicationCheckInsLoadState = OneMedicationLoadState.LOADED
        } catch (error: Exception) {
            medicationCheckInsLoadState = OneMedicationLoadState.ERROR
            medicationCheckInsLoadError = error.message ?: "Could not load medication check-in history."
        }
    }

    suspend fun createMedicationPlan(
        name: String,
        dose: String,
        schedule: String,
        instructions: String,
        assignedCaregiverId: UUID? = null
    ): Boolean {
        val authenticatedSession = session
        val careRecipientId = selectedCareRecipientId
        if (!backendMode || authenticatedSession == null) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Connect a backend session before creating a medication plan."
            return false
        }
        if (!canManageFamily) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Only caregivers can create medication plans."
            return false
        }
        if (careRecipientId == null) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Select a person before creating a medication plan."
            return false
        }
        if (recipientConsentIsKnownAndDenied("medication_management", careRecipientId)) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Active medication_management consent is required for this person."
            return false
        }
        val cleanName = name.trim()
        val cleanDose = dose.trim()
        val cleanSchedule = schedule.trim()
        val cleanInstructions = instructions.trim()
        if (cleanName.isBlank() || cleanDose.isBlank() || cleanSchedule.isBlank()) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Name, dose and schedule are required."
            return false
        }
        medicationPlanActionState = OneMedicationPlanActionState.SUBMITTING
        medicationPlanActionError = null
        try {
            lastMedicationPlan = apiClient.createMedicationPlan(
                authenticatedSession,
                MedicationPlanRequest(
                    careRecipientId = careRecipientId,
                    name = cleanName,
                    dose = cleanDose,
                    schedule = cleanSchedule,
                    instructions = cleanInstructions,
                    assignedCaregiverId = assignedCaregiverId
                )
            )
            medicationPlanActionState = OneMedicationPlanActionState.LOADED
            loadMedicationReminders(careRecipientId = careRecipientId)
            loadMedicationPlans(careRecipientId = careRecipientId)
            return true
        } catch (error: Exception) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = error.message ?: "Could not create the medication plan."
            return false
        }
    }

    suspend fun updateMedicationPlan(
        plan: OneMedicationPlan,
        name: String,
        dose: String,
        schedule: String,
        instructions: String,
        active: Boolean,
        assignedCaregiverId: UUID? = null
    ): Boolean {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Connect a backend session before updating a medication plan."
            return false
        }
        if (!canManageFamily) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Only caregivers can update medication plans."
            return false
        }
        if (plan.careRecipientId?.let { recipientConsentIsKnownAndDenied("medication_management", it) }
                ?: consentIsKnownAndDenied("medication_management", plan.subjectUserId)) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Active medication_management consent is required for this person."
            return false
        }
        val cleanName = name.trim()
        val cleanDose = dose.trim()
        val cleanSchedule = schedule.trim()
        val cleanInstructions = instructions.trim()
        if (cleanName.isBlank() || cleanDose.isBlank() || cleanSchedule.isBlank()) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = "Name, dose and schedule are required."
            return false
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
                    assignedCaregiverId = assignedCaregiverId,
                    version = plan.version
                )
            )
            // Remove alarms for the old rule before loading the new set. The
            // response may contain a different time, an inactive plan, or no
            // reminder at all; leaving the old PendingIntent would otherwise
            // notify the resident with stale medication details.
            OneMedicationScheduler.cancel(appContext, plan.id)
            medicationPlanActionState = OneMedicationPlanActionState.LOADED
            loadMedicationReminders(subjectUserId = plan.subjectUserId, careRecipientId = plan.careRecipientId)
            loadMedicationPlans(subjectUserId = plan.subjectUserId, careRecipientId = plan.careRecipientId)
            return true
        } catch (error: Exception) {
            medicationPlanActionState = OneMedicationPlanActionState.ERROR
            medicationPlanActionError = error.message ?: "Could not update the medication plan."
            return false
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
        if (selectedCareRecipientId?.let { recipientConsentIsKnownAndDenied("medication_management", it) }
                ?: consentIsKnownAndDenied("medication_management", selectedFamilySubjectId ?: authenticatedSession.userId)) {
            medicationActionError = "Active medication_management consent is required for this person."
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
            // Reconcile this plan after the status update. This removes the
            // fired/edited occurrence and lets the next recurring reminder be
            // scheduled from the server's authoritative response.
            OneMedicationScheduler.cancel(appContext, planId)
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
                apiClient.submitCheckIn(
                    authenticatedSession,
                    cleanTranscript,
                    careRecipientId = selectedCareRecipientId
                )
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
        val careRecipientId = selectedCareRecipientId
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
        if (careRecipientId == null && subjectUserId == null) {
            familyAssistantLoadState = OneFamilyAssistantLoadState.ERROR
            familyAssistantLoadError = "Select a person before asking the family assistant."
            return
        }
        if (careRecipientId?.let { recipientConsentIsKnownAndDenied("family_assistant", it) }
                ?: consentIsKnownAndDenied("family_assistant", subjectUserId)) {
            familyAssistantLoadState = OneFamilyAssistantLoadState.ERROR
            familyAssistantLoadError = "Active family_assistant consent is required for this person."
            return
        }
        familyAssistantLoadState = OneFamilyAssistantLoadState.SUBMITTING
        familyAssistantLoadError = null
        try {
            familyAssistantResult = apiClient.familyAssistant(
                session = authenticatedSession,
                message = message.trim().ifBlank { "Provide a concise administrative summary." },
                subjectUserId = if (careRecipientId == null) subjectUserId else null,
                careRecipientId = careRecipientId
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
            val latestBySubject = linkedMapOf<UUID, LinkedHashMap<String, Boolean>>()
            val latestByRecipient = linkedMapOf<UUID, LinkedHashMap<String, Boolean>>()
            apiClient.homeConsents(authenticatedSession).forEach { consent ->
                val subject = latestBySubject.getOrPut(consent.subjectUserId) { linkedMapOf() }
                if (!subject.containsKey(consent.purpose)) {
                    subject[consent.purpose] = consent.revokedAt == null
                }
                consent.careRecipientId?.let { recipientId ->
                    val recipient = latestByRecipient.getOrPut(recipientId) { linkedMapOf() }
                    if (!recipient.containsKey(consent.purpose)) recipient[consent.purpose] = consent.revokedAt == null
                }
            }
            consentStatesBySubject = latestBySubject
            consentStatesByCareRecipient = latestByRecipient
            consentStates = latestBySubject[authenticatedSession.userId].orEmpty()
            consentLoadState = OneConsentLoadState.LOADED
            loadCareEntries()
        } catch (error: Exception) {
            consentLoadState = OneConsentLoadState.ERROR
            consentLoadError = error.message ?: "Could not load privacy settings."
        }
    }

    suspend fun updateConsent(purpose: String, granted: Boolean, subjectUserId: UUID? = session?.userId) {
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
                    granted = granted,
                    subjectUserId = subjectUserId
                )
            )
            val subject = subjectUserId ?: authenticatedSession.userId
            val updatedForSubject = (consentStatesBySubject[subject].orEmpty() + (purpose to granted))
            consentStatesBySubject = consentStatesBySubject + (subject to updatedForSubject)
            if (subject == authenticatedSession.userId) consentStates = updatedForSubject
            if (!granted) {
                when (purpose) {
                    "video_capture" -> {
                        OneCaptureService.stop(appContext)
                        OneLiveKitPublisherService.stop(appContext)
                    }
                    "medication_management" -> {
                        OneMedicationScheduler.cancelAll(appContext)
                        if (subject == selectedFamilySubjectId || subject == authenticatedSession.userId) {
                            medicationDoses = null
                            medicationPlans = null
                            medicationCheckIns = null
                        }
                    }
                    "family_mode" -> {
                        if (subject == authenticatedSession.userId) familyMembers = null
                    }
                    "family_assistant" -> {
                        if (subject == selectedFamilySubjectId) familyAssistantResult = null
                    }
                }
            }
            consentLoadState = OneConsentLoadState.LOADED
        } catch (error: Exception) {
            consentUpdateError = error.message ?: "Could not update this privacy setting."
        } finally {
            if (consentUpdatePurpose == purpose) consentUpdatePurpose = null
        }
    }

    suspend fun updateCareRecipientConsent(purpose: String, granted: Boolean, careRecipientId: UUID) {
        val authenticatedSession = session
        if (!backendMode || authenticatedSession == null || !canManageFamily) return
        consentUpdatePurpose = purpose
        consentUpdateError = null
        try {
            apiClient.recordConsent(
                authenticatedSession,
                ConsentRequest(
                    purpose = purpose,
                    policyVersion = "2026-09",
                    granted = granted,
                    careRecipientId = careRecipientId
                )
            )
            val updated = consentStatesByCareRecipient[careRecipientId].orEmpty() + (purpose to granted)
            consentStatesByCareRecipient = consentStatesByCareRecipient + (careRecipientId to updated)
            if (!granted && purpose == "care_planning") {
                OneCareAppointmentScheduler.sync(appContext, careRecipientId, emptyList())
                if (careRecipientId == selectedCareRecipientId) careEntries = null
            }
            if (!granted && careRecipientId == selectedCareRecipientId) {
                when (purpose) {
                    "medication_management" -> {
                        OneMedicationScheduler.cancelAll(appContext)
                        medicationDoses = null
                        medicationPlans = null
                        medicationCheckIns = null
                    }
                    "family_assistant" -> familyAssistantResult = null
                    "outside_location" -> OneOutsideLocationService.stop(appContext, careRecipientId)
                }
            } else if (granted) {
                if (purpose == "analytics" && careRecipientId == selectedCareRecipientId) loadCareAnalytics(careRecipientId)
                if (purpose == "care_planning") {
                    if (careRecipientId == selectedCareRecipientId) loadCareEntries(careRecipientId)
                    else runCatching { apiClient.careEntries(authenticatedSession, careRecipientId) }
                        .onSuccess { OneCareAppointmentScheduler.sync(appContext, careRecipientId, it) }
                }
            }
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
        OneCaptureService.stop(appContext)
        OneLiveKitPublisherService.stop(appContext)
        OneMedicationScheduler.cancelAll(appContext)
        OneCareAppointmentScheduler.clear(appContext)
        withContext(Dispatchers.IO) { runCatching { offlineCache.clear() } }
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
        homeIsStale = false
        rooms = null
        objectActionState = OneObjectActionState.IDLE
        objectActionError = null
        lastCreatedObject = null
        observationActionState = OneObservationActionState.IDLE
        observationActionError = null
        lastObservation = null
        eventStreamState = OneEventStreamState.IDLE
        eventStreamError = null
        cameras = null
        cameraLoadState = OneCameraLoadState.IDLE
        cameraLoadError = null
        camerasAreStale = false
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
        selectedCareRecipientId = null
        careRecipientSelectionInitialized = false
        medicationDoses = null
        medicationLoadState = OneMedicationLoadState.IDLE
        medicationLoadError = null
        medicationPlans = null
        medicationPlansLoadState = OneMedicationLoadState.IDLE
        medicationPlansLoadError = null
        medicationPlanActionState = OneMedicationPlanActionState.IDLE
        lastMedicationPlan = null
        medicationPlanActionError = null
        careEntries = null
        careEntriesError = null
        careEntryActionError = null
        medicationActionKey = null
        medicationActionError = null
        assistantLoadState = OneAssistantLoadState.IDLE
        assistantResult = null
        assistantLoadError = null
        familyAssistantLoadState = OneFamilyAssistantLoadState.IDLE
        familyAssistantResult = null
        familyAssistantLoadError = null
        consentStates = null
        consentStatesBySubject = emptyMap()
        consentStatesByCareRecipient = emptyMap()
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
        publisherPairing = null
        publisherPairingLoadState = OneFamilyInviteLoadState.IDLE
        publisherPairingError = null
        cameraPairingStatus = null
        cameraPairingStatusLoadState = OneCameraPairingStatusLoadState.IDLE
        cameraPairingStatusError = null
        cameraReconnectLink = null
        cameraReconnectLoadState = OneCameraReconnectLoadState.IDLE
        cameraReconnectError = null
        careSpaces = null
        careSpacesLoadState = OneCareSpaceLoadState.IDLE
        careSpacesLoadError = null
        careSpaceActionState = OneCareSpaceActionState.IDLE
        careSpaceActionError = null
        careRecipients = null
        careRecipientsLoadState = OneCareRecipientLoadState.IDLE
        careRecipientsLoadError = null
        careRecipientActionState = OneCareRecipientActionState.IDLE
        careRecipientActionError = null
        familyMemberActionState = OneFamilyMemberActionState.IDLE
        familyMemberActionError = null
        familyMemberActionId = null
        medicationCheckIns = null
        medicationCheckInsLoadState = OneMedicationLoadState.IDLE
        medicationCheckInsLoadError = null
        medicationHistoryDays = 7
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
