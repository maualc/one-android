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
    private val medicationRepository: OneMedicationRepository
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
    var session by mutableStateOf<OneSession?>(null)
    var homeSnapshot by mutableStateOf<OneHomeSnapshot?>(null)
    var homeLoadState by mutableStateOf(OneHomeLoadState.IDLE)
    var homeLoadError by mutableStateOf<String?>(null)
    var eventStreamState by mutableStateOf(OneEventStreamState.IDLE)
    var eventStreamError by mutableStateOf<String?>(null)
    var cameras by mutableStateOf<List<OneCamera>?>(null)
    var cameraLoadState by mutableStateOf(OneCameraLoadState.IDLE)
    var cameraLoadError by mutableStateOf<String?>(null)
    var familyMembers by mutableStateOf<List<OneFamilyMember>?>(null)
    var familyLoadState by mutableStateOf(OneFamilyLoadState.IDLE)
    var familyLoadError by mutableStateOf<String?>(null)
    var medicationDoses by mutableStateOf<List<MedicationDose>?>(null)
    var medicationLoadState by mutableStateOf(OneMedicationLoadState.IDLE)
    var medicationLoadError by mutableStateOf<String?>(null)

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
        homeSnapshot = null
        homeLoadState = OneHomeLoadState.IDLE
        homeLoadError = null
        eventStreamState = OneEventStreamState.IDLE
        eventStreamError = null
        cameras = null
        cameraLoadState = OneCameraLoadState.IDLE
        cameraLoadError = null
        familyMembers = null
        familyLoadState = OneFamilyLoadState.IDLE
        familyLoadError = null
        medicationDoses = null
        medicationLoadState = OneMedicationLoadState.IDLE
        medicationLoadError = null
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
            cameraLoadState = OneCameraLoadState.LOADED
        } catch (error: Exception) {
            cameraLoadState = OneCameraLoadState.ERROR
            cameraLoadError = error.message ?: "Could not load the household cameras."
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
            familyLoadState = OneFamilyLoadState.LOADED
        } catch (error: Exception) {
            familyLoadState = OneFamilyLoadState.ERROR
            familyLoadError = error.message ?: "Could not load the care circle."
        }
    }

    suspend fun loadMedicationReminders() {
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
            medicationDoses = medicationRepository.load(authenticatedSession)
            medicationLoadState = OneMedicationLoadState.LOADED
        } catch (error: Exception) {
            medicationLoadState = OneMedicationLoadState.ERROR
            medicationLoadError = error.message ?: "Could not load medication reminders."
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
        homeSnapshot = null
        homeLoadState = OneHomeLoadState.IDLE
        homeLoadError = null
        eventStreamState = OneEventStreamState.IDLE
        eventStreamError = null
        cameras = null
        cameraLoadState = OneCameraLoadState.IDLE
        cameraLoadError = null
        familyMembers = null
        familyLoadState = OneFamilyLoadState.IDLE
        familyLoadError = null
        medicationDoses = null
        medicationLoadState = OneMedicationLoadState.IDLE
        medicationLoadError = null
        authStageName = AuthStage.SIGNED_OUT.name
    }
}
