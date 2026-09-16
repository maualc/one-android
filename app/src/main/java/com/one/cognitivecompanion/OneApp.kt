package com.one.cognitivecompanion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.one.cognitivecompanion.ui.theme.OneAmber
import com.one.cognitivecompanion.ui.theme.OneBlue
import com.one.cognitivecompanion.ui.theme.OneCyan
import com.one.cognitivecompanion.ui.theme.OneInverseSurface
import com.one.cognitivecompanion.ui.theme.OneMint
import com.one.cognitivecompanion.ui.theme.ONETheme
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.track.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import kotlin.math.min

private data class OneNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

private val caregiverTabs = listOf(
    OneNavItem("home", "Home", Icons.Default.Home),
    OneNavItem("map", "Map", Icons.Default.Map),
    OneNavItem("family", "Family", Icons.Default.People),
    OneNavItem("assistant", "Assistant", Icons.Default.GraphicEq),
    OneNavItem("account", "Account", Icons.Default.AccountCircle)
)

private val residentTabs = listOf(
    OneNavItem("today", "Today", Icons.Default.WbSunny),
    OneNavItem("assistant", "Assistant", Icons.Default.GraphicEq),
    OneNavItem("account", "Account", Icons.Default.AccountCircle)
)

private val publisherTabs = listOf(
    OneNavItem("publisher", "Publisher", Icons.Default.Visibility)
)

/**
 * Shared ONE styling for every modal panel in the app.
 *
 * Keeping this in one place prevents individual flows from falling back to
 * Material's default lavender dialog surface while leaving their actions and
 * data handling untouched.
 */
@Composable
private fun OneAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable (() -> Unit))? = null,
    icon: (@Composable (() -> Unit))? = null,
    title: (@Composable (() -> Unit))? = null,
    text: (@Composable (() -> Unit))? = null
) {
    val dialogShapes = MaterialTheme.shapes.copy(small = RoundedCornerShape(14.dp))
    MaterialTheme(shapes = dialogShapes) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier.padding(horizontal = 8.dp),
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            iconContentColor = OneBlue,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp
        )
    }
}

@Composable
private fun OneDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    menuWidth: Dp? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = if (menuWidth != null) modifier.width(menuWidth) else modifier.widthIn(min = 240.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content
    )
}

@Composable
fun OneApp() {
    // Demo mode starts inside the app so the shell is immediately usable. The
    // Account screen exposes a signed-out preview for demo and backend auth.
    val appContext = LocalContext.current.applicationContext
    val secureStore = remember(appContext) { OneSecureStore(appContext) }
    val offlineCache = remember(appContext) { OneOfflineCache(appContext) }
    val apiClient = remember { OneHttpApiClient() }
    val homeRepository = remember(apiClient) { OneApiHomeRepository(apiClient) }
    val cameraRepository = remember(apiClient) { OneApiCameraRepository(apiClient) }
    val familyRepository = remember(apiClient) { OneApiFamilyRepository(apiClient) }
    val medicationRepository = remember(apiClient) { OneApiMedicationRepository(apiClient) }
    val clipRepository = remember(apiClient) { OneApiClipRepository(apiClient) }
    val appState = remember(secureStore, apiClient, homeRepository, cameraRepository, familyRepository, medicationRepository, clipRepository, offlineCache) {
        OneAppState(appContext, apiClient, secureStore, homeRepository, cameraRepository, familyRepository, medicationRepository, clipRepository, offlineCache)
    }
    var authStageName by appState::authStageName
    var roleName by appState::roleName
    var selectedTab by appState::selectedTab
    var onboardingStep by appState::onboardingStep
    var onboardingConsentRoom by appState::onboardingConsentRoom
    var onboardingConsentMic by appState::onboardingConsentMic
    var onboardingConsentMedication by appState::onboardingConsentMedication
    var onboardingConsentFamily by appState::onboardingConsentFamily
    var onboardingConsentFamilyAssistant by appState::onboardingConsentFamilyAssistant
    var selectedCameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var captureCameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var liveKitPublishing by rememberSaveable { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<OneEvent?>(null) }
    var showEvents by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val signOut: () -> Unit = {
        selectedCameraId = null
        selectedEvent = null
        showEvents = false
        captureCameraId = null
        liveKitPublishing = false
        OneOutsideLocationService.stop(appContext)
        coroutineScope.launch { appState.signOut() }
    }
    val authStage = AuthStage.valueOf(authStageName)
    val role = OneRole.valueOf(roleName)
    val tabs = when (role) {
        OneRole.CAREGIVER -> caregiverTabs
        OneRole.RESIDENT -> residentTabs
        OneRole.PUBLISHER -> publisherTabs
    }
    val selectedCamera = appState.cameras?.firstOrNull { it.id.toString() == selectedCameraId }

    // Detail surfaces are an in-app navigation state. Handle the system back
    // gesture/button here so it closes the detail first instead of exiting to
    // the launcher.
    BackHandler(enabled = selectedCamera != null || selectedEvent != null || showEvents) {
        when {
            selectedCamera != null -> selectedCameraId = null
            selectedEvent != null -> selectedEvent = null
            else -> showEvents = false
        }
    }

    LaunchedEffect(appState) { appState.restoreSession() }
    LaunchedEffect(appState, appState.authStageName, appState.session) {
        if (appState.authStageName == AuthStage.AUTHENTICATED.name && appState.roleName != OneRole.PUBLISHER.name) {
            appState.loadConsents()
            appState.loadCareSpaces()
            if (appState.roleName == OneRole.CAREGIVER.name) {
                appState.loadHome()
                appState.loadCameras()
                appState.loadFamily()
            }
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.roleName, appState.selectedTab, showEvents, selectedEvent != null) {
        if (
            appState.authStageName == AuthStage.AUTHENTICATED.name &&
            appState.roleName == OneRole.CAREGIVER.name &&
            (appState.selectedTab == "home" || showEvents || selectedEvent != null)
        ) {
            appState.loadClips()
            appState.observeHomeEvents()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.selectedTab) {
        if (appState.authStageName == AuthStage.AUTHENTICATED.name && appState.selectedTab == "family") {
            appState.loadFamily()
            appState.loadCareRecipients()
            appState.loadMedicationReminders()
            appState.loadMedicationPlans()
            appState.loadMedicationCheckIns()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.roleName, appState.selectedTab) {
        if (
            appState.authStageName == AuthStage.AUTHENTICATED.name &&
            appState.roleName == OneRole.CAREGIVER.name &&
            appState.selectedTab == "map" &&
            appState.backendMode
        ) {
            appState.loadCareRecipients()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.roleName, appState.selectedTab) {
        if (
            appState.authStageName == AuthStage.AUTHENTICATED.name &&
            appState.roleName == OneRole.RESIDENT.name &&
            appState.selectedTab == "today"
        ) {
            appState.loadMedicationReminders()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.roleName, appState.selectedTab) {
        if (appState.authStageName == AuthStage.AUTHENTICATED.name && appState.selectedTab == "account") {
            appState.loadConsents()
            appState.checkBackendHealth()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.roleName, appState.selectedTab, appState.publisherPairing?.pairingId) {
        if (
            appState.authStageName == AuthStage.AUTHENTICATED.name &&
            appState.roleName == OneRole.CAREGIVER.name &&
            appState.selectedTab == "home" &&
            appState.publisherPairing != null
        ) {
            repeat(36) {
                appState.refreshCameraPairingStatus()
                if (appState.cameraPairingStatus?.status in setOf("connected", "expired")) return@LaunchedEffect
                delay(5_000)
            }
        }
    }

    if (authStage == AuthStage.AUTHENTICATED && tabs.none { it.key == selectedTab }) selectedTab = tabs.first().key

    Scaffold(
        bottomBar = if (authStage == AuthStage.AUTHENTICATED && selectedCamera == null && selectedEvent == null && !showEvents) {
            {
                OneBottomBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        } else {
            {}
        }
    ) { innerPadding ->
        OneBackground(modifier = Modifier.padding(innerPadding)) {
            when (authStage) {
                AuthStage.SIGNED_OUT -> LoginScreen(
                    apiClient = apiClient,
                    onAuthenticated = { authenticatedSession, usedBackend ->
                        appState.applyAuthenticatedSession(authenticatedSession, usedBackend)
                    }
                )
                AuthStage.ONBOARDING -> OnboardingScreen(
                    step = onboardingStep,
                    roomConsent = onboardingConsentRoom,
                    microphoneConsent = onboardingConsentMic,
                    medicationConsent = onboardingConsentMedication,
                    familyConsent = onboardingConsentFamily,
                    familyAssistantConsent = onboardingConsentFamilyAssistant,
                    onRoomConsentChange = { onboardingConsentRoom = it },
                    onMicrophoneConsentChange = { onboardingConsentMic = it },
                    onMedicationConsentChange = { onboardingConsentMedication = it },
                    onFamilyConsentChange = { onboardingConsentFamily = it },
                    onFamilyAssistantConsentChange = { onboardingConsentFamilyAssistant = it },
                    onContinue = {
                        if (onboardingStep == 1) appState.recordOnboardingConsents()
                        if (onboardingStep < 2) onboardingStep += 1
                        else appState.completeOnboarding()
                    }
                )
                AuthStage.AUTHENTICATED -> if (selectedCamera != null && role == OneRole.CAREGIVER) {
                    LiveCameraScreen(
                        camera = selectedCamera,
                        apiClient = apiClient,
                        session = appState.session,
                        onClose = { selectedCameraId = null }
                    )
                } else if (selectedEvent != null && role == OneRole.CAREGIVER) {
                    EventDetailScreen(
                        event = selectedEvent!!,
                        apiClient = apiClient,
                        session = appState.session,
                        clips = appState.clips,
                        clipLoadState = appState.clipLoadState,
                        clipLoadError = appState.clipLoadError,
                        onClose = { selectedEvent = null }
                    )
                } else if (showEvents && role == OneRole.CAREGIVER) {
                    EventsScreen(
                        events = appState.homeSnapshot?.events ?: if (appState.backendMode) emptyList() else demoEvents,
                        isBackend = appState.backendMode,
                        homeLoadState = appState.homeLoadState,
                        homeLoadError = appState.homeLoadError,
                        homeIsStale = appState.homeIsStale,
                        eventStreamState = appState.eventStreamState,
                        eventStreamError = appState.eventStreamError,
                        clips = appState.clips,
                        clipLoadState = appState.clipLoadState,
                        clipLoadError = appState.clipLoadError,
                        onRetry = { coroutineScope.launch { appState.loadHome() } },
                        onClipRetry = { coroutineScope.launch { appState.loadClips() } },
                        onOpenEvent = { selectedEvent = it },
                        onClose = { showEvents = false }
                    )
                } else {
                    val tabIndex = tabs.mapIndexed { index, tab -> tab.key to index }.toMap()
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if ((tabIndex[targetState] ?: 0) >= (tabIndex[initialState] ?: 0)) 1 else -1
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                                animationSpec = tween(260)
                            ) { fullWidth -> fullWidth / 6 * direction }) togetherWith
                                (fadeOut(animationSpec = tween(160)) + slideOutHorizontally(
                                    animationSpec = tween(220)
                                ) { fullWidth -> -fullWidth / 10 * direction })
                        },
                        label = "Bottom navigation screen transition"
                    ) { visibleTab ->
                        when (role) {
                            OneRole.PUBLISHER -> PublisherScreen(
                                isPublishing = liveKitPublishing,
                                hasReconnectLink = appState.session?.reconnectToken != null,
                                reconnectLoadState = appState.cameraReconnectLoadState,
                                reconnectError = appState.cameraReconnectError,
                                onCreateReconnectLink = { coroutineScope.launch { appState.createCameraReconnectLink() } },
                                onReconnect = { coroutineScope.launch { appState.reconnectPublisherCamera() } },
                                onSignOut = signOut,
                                onStart = {
                                    OneLiveKitPublisherService.start(appContext)
                                    liveKitPublishing = true
                                },
                                onStop = {
                                    OneLiveKitPublisherService.stop(appContext)
                                    liveKitPublishing = false
                                }
                            )
                            OneRole.CAREGIVER -> when (visibleTab) {
                        "map" -> OneOutsideTrackingScreen(
                            careRecipients = appState.careRecipients,
                            isBackend = appState.backendMode,
                            careRecipientsLoading = appState.careRecipientsLoadState == OneCareRecipientLoadState.LOADING,
                            onOpenFamily = { selectedTab = "family" }
                        )
                        "family" -> FamilyScreen(
                            members = appState.familyMembers,
                            currentUserId = appState.session?.userId,
                            isBackend = appState.backendMode,
                            loadState = appState.familyLoadState,
                            loadError = appState.familyLoadError,
                            onRetry = { coroutineScope.launch { appState.loadFamily() } },
                            canInvite = appState.canManageFamily && appState.consentStates?.get("family_mode") == true,
                            familyInviteLoadState = appState.familyInviteLoadState,
                            familyInvite = appState.familyInvite,
                            familyInviteLoadError = appState.familyInviteLoadError,
                            onCreateInvite = { invite -> coroutineScope.launch { appState.createFamilyInvite(invite) } },
                            careRecipients = appState.careRecipients,
                            careRecipientsLoadState = appState.careRecipientsLoadState,
                            careRecipientsLoadError = appState.careRecipientsLoadError,
                            careRecipientActionState = appState.careRecipientActionState,
                            careRecipientActionError = appState.careRecipientActionError,
                            canManageRecipients = appState.canManageFamily,
                            onCareRecipientsRetry = { coroutineScope.launch { appState.loadCareRecipients() } },
                            onCreateCareRecipient = { name, relationship, roomLabel ->
                                coroutineScope.launch { appState.createCareRecipient(name, relationship, roomLabel) }
                            },
                            onUpdateCareRecipient = { recipient, name, relationship, roomLabel ->
                                coroutineScope.launch { appState.updateCareRecipient(recipient, name, relationship, roomLabel) }
                            },
                            onDeleteCareRecipient = { recipient ->
                                coroutineScope.launch { appState.deleteCareRecipient(recipient) }
                            },
                            familyMemberActionState = appState.familyMemberActionState,
                            familyMemberActionError = appState.familyMemberActionError,
                            familyMemberActionId = appState.familyMemberActionId,
                            isAdmin = appState.isAdmin,
                            onUpdateFamilyMember = { member, memberRole ->
                                coroutineScope.launch { appState.updateFamilyMember(member, memberRole) }
                            },
                            onRemoveFamilyMember = { member ->
                                coroutineScope.launch { appState.removeFamilyMember(member) }
                            },
                            selectedFamilySubjectId = appState.selectedFamilySubjectId,
                            selectedSubjectMedicationConsent = appState.selectedFamilySubjectId?.let { appState.consentStatesBySubject[it]?.get("medication_management") } == true,
                            selectedSubjectFamilyConsent = appState.selectedFamilySubjectId?.let { appState.consentStatesBySubject[it]?.get("family_mode") } == true,
                            selectedSubjectAssistantConsent = appState.selectedFamilySubjectId?.let { appState.consentStatesBySubject[it]?.get("family_assistant") } == true,
                            onSelectFamilySubject = { subjectId -> coroutineScope.launch { appState.selectFamilySubject(subjectId) } },
                            medicationDoses = appState.medicationDoses,
                            medicationLoadState = appState.medicationLoadState,
                            medicationLoadError = appState.medicationLoadError,
                            onMedicationRetry = { coroutineScope.launch { appState.loadMedicationReminders() } },
                            medicationPlans = appState.medicationPlans,
                            medicationPlansLoadState = appState.medicationPlansLoadState,
                            medicationPlansLoadError = appState.medicationPlansLoadError,
                            onMedicationPlansRetry = { coroutineScope.launch { appState.loadMedicationPlans() } },
                            medicationCheckIns = appState.medicationCheckIns,
                            medicationCheckInsLoadState = appState.medicationCheckInsLoadState,
                            medicationCheckInsLoadError = appState.medicationCheckInsLoadError,
                            medicationHistoryDays = appState.medicationHistoryDays,
                            onMedicationHistoryRetry = { coroutineScope.launch { appState.loadMedicationCheckIns() } },
                            onMedicationHistoryRangeChange = { days -> coroutineScope.launch { appState.loadMedicationCheckIns(days = days) } },
                            medicationPlanActionState = appState.medicationPlanActionState,
                            lastMedicationPlan = appState.lastMedicationPlan,
                            medicationPlanActionError = appState.medicationPlanActionError,
                            onCreateMedicationPlan = { name, dose, schedule, instructions, assignedCaregiverId ->
                                coroutineScope.launch { appState.createMedicationPlan(name, dose, schedule, instructions, assignedCaregiverId) }
                            },
                            onUpdateMedicationPlan = { plan, name, dose, schedule, instructions, active, assignedCaregiverId ->
                                coroutineScope.launch {
                                    appState.updateMedicationPlan(plan, name, dose, schedule, instructions, active, assignedCaregiverId)
                                }
                            },
                            medicationActionKey = appState.medicationActionKey,
                            medicationActionError = appState.medicationActionError,
                            onMedicationStatusChange = { dose, status ->
                                coroutineScope.launch { appState.updateMedicationDose(dose, status) }
                            }
                        )
                        "assistant" -> CaregiverAssistantScreen(
                            isBackend = appState.backendMode,
                            members = appState.familyMembers,
                            selectedFamilySubjectId = appState.selectedFamilySubjectId,
                            selectedSubjectAssistantConsent = appState.selectedFamilySubjectId?.let {
                                appState.consentStatesBySubject[it]?.get("family_assistant")
                            } == true,
                            familyLoadState = appState.familyLoadState,
                            familyLoadError = appState.familyLoadError,
                            onFamilyRetry = { coroutineScope.launch { appState.loadFamily() } },
                            onSelectFamilySubject = { subjectId -> coroutineScope.launch { appState.selectFamilySubject(subjectId) } },
                            familyAssistantLoadState = appState.familyAssistantLoadState,
                            familyAssistantResult = appState.familyAssistantResult,
                            familyAssistantLoadError = appState.familyAssistantLoadError,
                            onFamilyAssistantSubmit = { message -> coroutineScope.launch { appState.submitFamilyAssistant(message) } }
                        )
                        "account" -> AccountScreen(
                            role = role,
                            isBackend = appState.backendMode,
                            backendHealth = appState.backendHealth,
                            backendHealthLoadState = appState.backendHealthLoadState,
                            backendHealthError = appState.backendHealthError,
                            onBackendHealthRetry = { coroutineScope.launch { appState.checkBackendHealth() } },
                            consentStates = appState.consentStates,
                            consentSubjectName = "your account",
                            consentLoadState = appState.consentLoadState,
                            consentLoadError = appState.consentLoadError,
                            consentUpdatePurpose = appState.consentUpdatePurpose,
                            consentUpdateError = appState.consentUpdateError,
                            onConsentRetry = { coroutineScope.launch { appState.loadConsents() } },
                            onConsentChange = { purpose, granted -> coroutineScope.launch { appState.updateConsent(purpose, granted) } },
                            exportLoadState = appState.exportLoadState,
                            dataExport = appState.dataExport,
                            exportLoadError = appState.exportLoadError,
                            onExport = { coroutineScope.launch { appState.requestDataExport() } },
                            isAdmin = appState.isAdmin,
                            deletionLoadState = appState.deletionLoadState,
                            dataDeletion = appState.dataDeletion,
                            deletionLoadError = appState.deletionLoadError,
                            onDelete = { coroutineScope.launch { appState.requestDataDeletion() } },
                            careSpaces = appState.careSpaces,
                            careSpacesLoadState = appState.careSpacesLoadState,
                            careSpacesLoadError = appState.careSpacesLoadError,
                            careSpaceActionState = appState.careSpaceActionState,
                            careSpaceActionError = appState.careSpaceActionError,
                            canCreateCareSpace = appState.canManageFamily,
                            onCareSpacesRetry = { coroutineScope.launch { appState.loadCareSpaces() } },
                            onCreateCareSpace = { name, setting, focus ->
                                coroutineScope.launch { appState.createCareSpace(name, setting, focus) }
                            },
                            onActivateCareSpace = { space -> coroutineScope.launch { appState.activateCareSpace(space) } },
                            onRoleChange = { roleName = it.name; selectedTab = if (it == OneRole.RESIDENT) "today" else "home" },
                            onSignOut = signOut
                        )
                        else -> CaregiverHomeScreen(
                            homeSnapshot = appState.homeSnapshot,
                            homeLoadState = appState.homeLoadState,
                            homeLoadError = appState.homeLoadError,
                            homeIsStale = appState.homeIsStale,
                            onRetry = { coroutineScope.launch { appState.loadHome() } },
                            isBackend = appState.backendMode,
                            currentUserName = appState.familyMembers?.firstOrNull { it.id == appState.session?.userId }?.displayName
                                ?: if (!appState.backendMode) "Biel Martínez" else null,
                            careSpaces = appState.careSpaces,
                            careSpacesLoadState = appState.careSpacesLoadState,
                            careSpacesLoadError = appState.careSpacesLoadError,
                            careSpaceActionState = appState.careSpaceActionState,
                            careSpaceActionError = appState.careSpaceActionError,
                            canCreateCareSpace = appState.canManageFamily,
                            onCareSpacesRetry = { coroutineScope.launch { appState.loadCareSpaces() } },
                            onCreateCareSpace = { name, setting, focus ->
                                coroutineScope.launch { appState.createCareSpace(name, setting, focus) }
                            },
                            onActivateCareSpace = { space -> coroutineScope.launch { appState.activateCareSpace(space) } },
                            medicationDoses = appState.medicationDoses,
                            medicationLoadState = appState.medicationLoadState,
                            medicationLoadError = appState.medicationLoadError,
                            onMedicationRetry = { coroutineScope.launch { appState.loadMedicationReminders() } },
                            cameras = appState.cameras,
                            camerasAreStale = appState.camerasAreStale,
                            rooms = appState.rooms,
                            cameraLoadState = appState.cameraLoadState,
                            cameraLoadError = appState.cameraLoadError,
                            cameraActionState = appState.cameraActionState,
                            cameraActionError = appState.cameraActionError,
                            onCameraRetry = { coroutineScope.launch { appState.loadCameras() } },
                            onRegisterCamera = { name, roomId ->
                                coroutineScope.launch { appState.registerCamera(name, roomId) }
                            },
                            onUpdateCamera = { camera, name, roomId, enabled ->
                                coroutineScope.launch { appState.updateCamera(camera, name, roomId, enabled) }
                            },
                            onOpenCamera = { selectedCameraId = it.id.toString() },
                            onOpenMap = { selectedTab = "map" },
                            onOpenPlan = { selectedTab = "family" },
                            onOpenEvents = { showEvents = true },
                            onOpenEvent = { selectedEvent = it; showEvents = true },
                            videoConsentGranted = appState.consentStates?.get("video_capture") == true,
                            publisherPairing = appState.publisherPairing,
                            publisherPairingLoadState = appState.publisherPairingLoadState,
                            publisherPairingError = appState.publisherPairingError,
                            onCreatePublisherPairing = { label -> coroutineScope.launch { appState.createPublisherPairing(label) } },
                            cameraPairingStatus = appState.cameraPairingStatus,
                            cameraPairingStatusLoadState = appState.cameraPairingStatusLoadState,
                            cameraPairingStatusError = appState.cameraPairingStatusError,
                            onRefreshCameraPairingStatus = { coroutineScope.launch { appState.refreshCameraPairingStatus() } },
                            captureCameraId = captureCameraId,
                            liveKitPublishing = liveKitPublishing,
                            onStartCapture = { camera ->
                                OneCaptureService.start(appContext, camera.id)
                                captureCameraId = camera.id.toString()
                            },
                            onStopCapture = {
                                OneCaptureService.stop(appContext)
                                captureCameraId = null
                            },
                            onStartLiveKit = {
                                OneLiveKitPublisherService.start(appContext)
                                liveKitPublishing = true
                            },
                            onStopLiveKit = {
                                OneLiveKitPublisherService.stop(appContext)
                                liveKitPublishing = false
                            }
                        )
                    }
                            OneRole.RESIDENT -> when (visibleTab) {
                        "today" -> ResidentTodayScreen(
                            isBackend = appState.backendMode,
                            medicationDoses = appState.medicationDoses,
                            medicationLoadState = appState.medicationLoadState,
                            medicationLoadError = appState.medicationLoadError,
                            onMedicationRetry = { coroutineScope.launch { appState.loadMedicationReminders() } },
                            onOpenAssistant = { selectedTab = "assistant" }
                        )
                        "assistant" -> AssistantScreen(
                            isBackend = appState.backendMode,
                            audioConsentGranted = appState.consentStates?.get("audio_capture") == true,
                            loadState = appState.assistantLoadState,
                            result = appState.assistantResult,
                            loadError = appState.assistantLoadError,
                            onSubmit = { transcript ->
                                coroutineScope.launch { appState.submitAssistantCheckIn(transcript) }
                            }
                        )
                        "account" -> AccountScreen(
                            role = role,
                            isBackend = appState.backendMode,
                            backendHealth = appState.backendHealth,
                            backendHealthLoadState = appState.backendHealthLoadState,
                            backendHealthError = appState.backendHealthError,
                            onBackendHealthRetry = { coroutineScope.launch { appState.checkBackendHealth() } },
                            consentStates = appState.consentStates,
                            consentSubjectName = "your account",
                            consentLoadState = appState.consentLoadState,
                            consentLoadError = appState.consentLoadError,
                            consentUpdatePurpose = appState.consentUpdatePurpose,
                            consentUpdateError = appState.consentUpdateError,
                            onConsentRetry = { coroutineScope.launch { appState.loadConsents() } },
                            onConsentChange = { purpose, granted -> coroutineScope.launch { appState.updateConsent(purpose, granted) } },
                            exportLoadState = appState.exportLoadState,
                            dataExport = appState.dataExport,
                            exportLoadError = appState.exportLoadError,
                            onExport = { coroutineScope.launch { appState.requestDataExport() } },
                            isAdmin = appState.isAdmin,
                            deletionLoadState = appState.deletionLoadState,
                            dataDeletion = appState.dataDeletion,
                            deletionLoadError = appState.deletionLoadError,
                            onDelete = { coroutineScope.launch { appState.requestDataDeletion() } },
                            careSpaces = appState.careSpaces,
                            careSpacesLoadState = appState.careSpacesLoadState,
                            careSpacesLoadError = appState.careSpacesLoadError,
                            careSpaceActionState = appState.careSpaceActionState,
                            careSpaceActionError = appState.careSpaceActionError,
                            canCreateCareSpace = appState.canManageFamily,
                            onCareSpacesRetry = { coroutineScope.launch { appState.loadCareSpaces() } },
                            onCreateCareSpace = { name, setting, focus ->
                                coroutineScope.launch { appState.createCareSpace(name, setting, focus) }
                            },
                            onActivateCareSpace = { space -> coroutineScope.launch { appState.activateCareSpace(space) } },
                            onRoleChange = { roleName = it.name; selectedTab = if (it == OneRole.RESIDENT) "today" else "home" },
                            onSignOut = signOut
                        )
                        else -> ResidentTodayScreen(
                            isBackend = appState.backendMode,
                            medicationDoses = appState.medicationDoses,
                            medicationLoadState = appState.medicationLoadState,
                            medicationLoadError = appState.medicationLoadError,
                            onMedicationRetry = { coroutineScope.launch { appState.loadMedicationReminders() } },
                            onOpenAssistant = { selectedTab = "assistant" }
                        )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun OneBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-75).dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), CircleShape)
        )
        content()
    }
}

@Composable
private fun ScreenScroll(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        content = { content() }
    )
}

@Composable
private fun ScreenHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.one_logo),
                contentDescription = "ONE logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OneBottomBar(
    tabs: List<OneNavItem>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = selectedTab == tab.key
                val tabBackgroundColor by animateColorAsState(
                    targetValue = if (selected) OneBlue else Color.Transparent,
                    animationSpec = tween(220),
                    label = "${tab.key} tab background"
                )
                val tabContentColor by animateColorAsState(
                    targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "${tab.key} tab content"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(tabBackgroundColor)
                        .clickable(role = Role.Tab) { onTabSelected(tab.key) }
                        .padding(horizontal = 4.dp, vertical = 7.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = tab.label
                            role = Role.Tab
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = tabContentColor
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tabContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun LoginScreen(
    apiClient: OneApiClient,
    onAuthenticated: (OneSession?, Boolean) -> Unit
) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var emailCode by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var homeName by rememberSaveable { mutableStateOf("") }
    var careSetting by rememberSaveable { mutableStateOf("home") }
    var supportFocus by rememberSaveable { mutableStateOf("general") }
    var accountConsent by rememberSaveable { mutableStateOf(false) }
    var useBackend by rememberSaveable { mutableStateOf(true) }
    var backendStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var emailChallenge by remember { mutableStateOf<EmailAuthChallenge?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isCreateMode = mode == 1
    val isEmailMode = mode == 0 || mode == 1
    val emailIsValid = email.trim().length >= 3 && email.trim().contains("@")
    val verificationCodeIsValid = emailCode.length == 6 && emailCode.all(Char::isDigit)
    val pairingCodeIsValid = pairingCode.length == 6 && pairingCode.all(Char::isDigit)
    val canRequestEmail = emailIsValid && (!isCreateMode || (name.isNotBlank() && homeName.isNotBlank() && accountConsent))
    val canContinue = when {
        !useBackend -> !isCreateMode || (name.isNotBlank() && homeName.isNotBlank() && accountConsent)
        isEmailMode -> if (emailChallenge == null) canRequestEmail else verificationCodeIsValid
        else -> pairingCodeIsValid
    }

    LaunchedEffect(useBackend) {
        errorMessage = null
        emailChallenge = null
        emailCode = ""
        if (!useBackend) {
            backendStatus = null
        } else {
            backendStatus = "Checking backend…"
            backendStatus = runCatching {
                val health = apiClient.health()
                if (health.status == "ok") "Backend connected" else "Backend unavailable"
            }.getOrElse { "Backend unavailable" }
        }
    }

    ScreenScroll {
        Spacer(Modifier.height(34.dp))
        ScreenHeader(
            eyebrow = "ONE",
            title = "Sign in to your home.",
            subtitle = "Use the one-time code from your ONE backend. Your session will be stored securely on this device."
        )
        PrimaryTabRow(selectedTabIndex = mode) {
            Tab(selected = mode == 0, onClick = { mode = 0; emailChallenge = null; emailCode = "" }, text = { Text("Sign in") })
            Tab(selected = mode == 1, onClick = { mode = 1; emailChallenge = null; emailCode = "" }, text = { Text("Create") })
            Tab(selected = mode == 2, onClick = { mode = 2; emailChallenge = null; emailCode = "" }, text = { Text("Join") })
            Tab(selected = mode == 3, onClick = { mode = 3; emailChallenge = null; emailCode = "" }, text = { Text("Pair") })
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Use ONE backend", style = MaterialTheme.typography.titleMedium)
                Text("Connect this device to a running FastAPI home.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = useBackend, onCheckedChange = { useBackend = it })
        }
        backendStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (it == "Backend connected") OneMint else MaterialTheme.colorScheme.onSurfaceVariant) }
        if (isCreateMode && emailChallenge == null) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailChallenge = null; emailCode = "" },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = homeName, onValueChange = { homeName = it }, label = { Text("Household name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Care setting", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = careSetting == "home", onClick = { careSetting = "home" }, label = { Text("Home") })
                FilterChip(selected = careSetting == "residence", onClick = { careSetting = "residence" }, label = { Text("Residence") })
            }
            Text("Support focus", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = supportFocus == "general", onClick = { supportFocus = "general" }, label = { Text("Everyday") })
                FilterChip(selected = supportFocus == "mci", onClick = { supportFocus = "mci" }, label = { Text("Memory-focused") })
            }
            ConsentRow("I consent to ONE storing the account data needed for this service.", accountConsent) { accountConsent = it }
        } else if (isEmailMode) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailChallenge = null; emailCode = "" },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            emailChallenge?.let { challenge ->
                Text("A one-time code was sent to ${challenge.email}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = emailCode,
                    onValueChange = { emailCode = it.filter(Char::isDigit).take(6) },
                    label = { Text("Email verification code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!BuildConfig.ONE_PRODUCTION_BUILD) {
                    challenge.devCode?.let { code ->
                        Text("Development code: $code", style = MaterialTheme.typography.bodySmall, color = OneAmber)
                    }
                }
                TextButton(
                    onClick = { emailChallenge = null; emailCode = ""; errorMessage = null },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Use another email") }
            }
        } else {
            OutlinedTextField(
                value = pairingCode,
                onValueChange = { pairingCode = it.uppercase() },
                label = { Text(if (mode == 2) "Invitation code" else "Publisher pairing code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (mode == 2) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = OneAmber) }
        Text(
            if (useBackend && isEmailMode) "Email codes are six digits and are used only once." else if (useBackend && mode == 2) "Invitation codes are six digits and are used only once." else if (useBackend) "Publisher pairing codes are six digits and are used only once." else "Demo mode is active. Continue without a server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = {
                if (!useBackend) {
                    onAuthenticated(null, false)
                } else {
                    coroutineScope.launch {
                        isSubmitting = true
                        errorMessage = null
                        try {
                            if (isEmailMode) {
                                if (emailChallenge == null) {
                                    emailChallenge = apiClient.requestEmailCode(
                                        EmailAuthRequest(
                                            email = email.trim(),
                                            purpose = if (isCreateMode) "create" else "login",
                                            displayName = name.trim().ifBlank { null },
                                            homeName = homeName.trim().ifBlank { "ONE Home" },
                                            careSetting = careSetting,
                                            supportFocus = supportFocus
                                        )
                                    )
                                } else {
                                    onAuthenticated(apiClient.verifyEmailCode(EmailAuthVerifyRequest(email.trim(), emailCode)), true)
                                }
                            } else {
                                val authenticated = if (mode == 2) {
                                    apiClient.acceptFamilyInvite(FamilyInviteAcceptRequest(pairingCode, name.trim().ifBlank { null }))
                                } else {
                                    apiClient.completePairing(pairingCode)
                                }
                                onAuthenticated(authenticated, true)
                            }
                        } catch (error: Exception) {
                            errorMessage = error.message ?: "Could not connect to the ONE backend."
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            },
            enabled = canContinue && !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OneBlue)
        ) {
            Text(
                if (isSubmitting) "Working…"
                else if (emailChallenge != null) "Verify code"
                else if (isEmailMode) "Send code"
                else if (mode == 0) "Sign in"
                else if (mode == 1) "Create account"
                else if (mode == 2) "Join household"
                else "Pair device",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(9.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun OnboardingScreen(
    step: Int,
    roomConsent: Boolean,
    microphoneConsent: Boolean,
    medicationConsent: Boolean,
    familyConsent: Boolean,
    familyAssistantConsent: Boolean,
    onRoomConsentChange: (Boolean) -> Unit,
    onMicrophoneConsentChange: (Boolean) -> Unit,
    onMedicationConsentChange: (Boolean) -> Unit,
    onFamilyConsentChange: (Boolean) -> Unit,
    onFamilyAssistantConsentChange: (Boolean) -> Unit,
    onContinue: suspend () -> Unit
) {
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    ScreenScroll {
        Spacer(Modifier.height(34.dp))
        ScreenHeader("WELCOME", when (step) {
            0 -> "Welcome to your home."
            1 -> "Choose what ONE may use."
            else -> "Set up at your pace."
        }, when (step) {
            0 -> "ONE helps your care circle notice daily rhythms with clarity and consent."
            1 -> "You can change these choices later in Account."
            else -> "Camera pairing and inviting family are optional. You can do them later."
        })
        when (step) {
            0 -> {
                InfoCard("Your choices stay yours.", "ONE is designed to support independence, not replace the person or their care team.")
                InfoCard("Observations are not diagnoses.", "Signals are shown with context and uncertainty for human review.")
            }
            1 -> {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ConsentRow("Daily check-in support", microphoneConsent) { onMicrophoneConsentChange(it) }
                        HorizontalDivider()
                        ConsentRow("Room and camera data", roomConsent) { onRoomConsentChange(it) }
                        HorizontalDivider()
                        ConsentRow("Medication reminders", medicationConsent) { onMedicationConsentChange(it) }
                        HorizontalDivider()
                        ConsentRow("Family sharing", familyConsent) { onFamilyConsentChange(it) }
                        HorizontalDivider()
                        ConsentRow("Family assistant summaries", familyAssistantConsent) { onFamilyAssistantConsentChange(it) }
                    }
                }
            }
            else -> {
                InfoCard("No access is enabled automatically.", "Camera pairing and family invitations remain optional until you choose them from the care circle.")
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("You are in control of what is recorded, stored, and shared.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = OneAmber) }
        Button(
            onClick = {
                coroutineScope.launch {
                    isSubmitting = true
                    errorMessage = null
                    try {
                        onContinue()
                    } catch (error: Exception) {
                        errorMessage = error.message ?: "Could not save your choices."
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OneBlue)
        ) {
            Text(if (isSubmitting) "Saving…" else if (step < 2) "Continue" else "Finish setup", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
@kotlin.OptIn(ExperimentalMaterial3Api::class)
private fun CaregiverHomeScreen(
    homeSnapshot: OneHomeSnapshot?,
    homeLoadState: OneHomeLoadState,
    homeLoadError: String?,
    homeIsStale: Boolean,
    onRetry: () -> Unit,
    isBackend: Boolean,
    currentUserName: String?,
    careSpaces: List<OneCareSpace>?,
    careSpacesLoadState: OneCareSpaceLoadState,
    careSpacesLoadError: String?,
    careSpaceActionState: OneCareSpaceActionState,
    careSpaceActionError: String?,
    canCreateCareSpace: Boolean,
    onCareSpacesRetry: () -> Unit,
    onCreateCareSpace: (String, String, String) -> Unit,
    onActivateCareSpace: (OneCareSpace) -> Unit,
    medicationDoses: List<MedicationDose>?,
    medicationLoadState: OneMedicationLoadState,
    medicationLoadError: String?,
    onMedicationRetry: () -> Unit,
    cameras: List<OneCamera>?,
    camerasAreStale: Boolean,
    rooms: List<OneRoom>?,
    cameraLoadState: OneCameraLoadState,
    cameraLoadError: String?,
    cameraActionState: OneCameraActionState,
    cameraActionError: String?,
    onCameraRetry: () -> Unit,
    onRegisterCamera: (String, UUID?) -> Unit,
    onUpdateCamera: (OneCamera, String, UUID?, Boolean) -> Unit,
    onOpenCamera: (OneCamera) -> Unit,
    onOpenMap: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenEvent: (OneEvent) -> Unit,
    videoConsentGranted: Boolean,
    publisherPairing: PublisherPairingStartResponse?,
    publisherPairingLoadState: OneFamilyInviteLoadState,
    publisherPairingError: String?,
    onCreatePublisherPairing: (String) -> Unit,
    cameraPairingStatus: OneCameraPairingStatus?,
    cameraPairingStatusLoadState: OneCameraPairingStatusLoadState,
    cameraPairingStatusError: String?,
    onRefreshCameraPairingStatus: () -> Unit,
    captureCameraId: String?,
    liveKitPublishing: Boolean,
    onStartCapture: (OneCamera) -> Unit,
    onStopCapture: () -> Unit,
    onStartLiveKit: () -> Unit,
    onStopLiveKit: () -> Unit
) {
    var showCareSpaces by rememberSaveable { mutableStateOf(false) }
    var showCameraSetup by rememberSaveable { mutableStateOf(false) }
    val isBackendHome = isBackend
    val events = homeSnapshot?.events ?: if (isBackendHome) emptyList() else demoEvents
    val doses = (if (isBackendHome) medicationDoses.orEmpty() else demoMedicationDoses)
        .sortedWith(compareBy<MedicationDose> { it.scheduledFor ?: Instant.MAX }.thenBy { it.time })
    val nextDose = doses.firstOrNull {
        it.status !in setOf(DoseStatus.ACKNOWLEDGED, DoseStatus.TAKEN, DoseStatus.SKIPPED)
    } ?: doses.firstOrNull()
    val activeCareSpace = careSpaces?.firstOrNull { it.active } ?: careSpaces?.firstOrNull()
    val firstName = currentUserName
        ?.trim()
        ?.split(" ")
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
    val greeting = firstName?.let { "Welcome back, $it" } ?: "Welcome back"
    val residentName = homeSnapshot?.profile?.residentName?.takeIf { it.isNotBlank() }
    val firstCheckIn = events.firstOrNull { it.kind == EventKind.CHECK_IN }
    val hasOnlineCamera = cameras.orEmpty().any { it.status.equals("online", ignoreCase = true) }

    ScreenScroll {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(greeting, style = MaterialTheme.typography.headlineLarge)
                Text(
                    residentName?.let { "Here’s what matters for $it today." }
                        ?: "A calm, human-readable picture of today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Image(
                painter = painterResource(R.drawable.one_logo),
                contentDescription = "ONE logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(34.dp)
            )
        }

        CareSpaceContextCard(
            space = activeCareSpace,
            isLoading = careSpacesLoadState == OneCareSpaceLoadState.LOADING,
            onClick = { showCareSpaces = true }
        )

        if (homeIsStale) {
            AssistChip(onClick = onRetry, label = { Text("Offline · showing last known data") })
        }
        if (!isBackendHome) {
            AssistChip(onClick = { }, enabled = false, label = { Text("Demo preview · synthetic home data") })
        }

        when {
            homeLoadState == OneHomeLoadState.LOADING && homeSnapshot == null -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            homeLoadState == OneHomeLoadState.ERROR && homeSnapshot == null -> {
                InfoCard("Home data unavailable", homeLoadError ?: "ONE could not reach the household right now.")
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
        }

        HomeTodayCard(
            nextDose = nextDose,
            eventCount = events.size,
            checkInTime = firstCheckIn?.time,
            isMedicationLoading = medicationLoadState == OneMedicationLoadState.LOADING && isBackendHome,
            onOpenPlan = onOpenPlan
        )
        if (isBackendHome && medicationLoadState == OneMedicationLoadState.ERROR && medicationDoses == null) {
            Text(
                medicationLoadError ?: "ONE could not load today's medication plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = onMedicationRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry today's plan") }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading("HOME", "At a glance")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeGlanceCard(
                    modifier = Modifier.weight(1f),
                    title = "Map",
                    detail = when {
                        homeSnapshot?.objects?.isNotEmpty() == true -> "${homeSnapshot.objects.size} mapped objects"
                        else -> "Set up your home"
                    },
                    icon = Icons.Default.Map,
                    status = if (homeSnapshot?.objects?.isNotEmpty() == true) OneMint else OneBlue,
                    onClick = onOpenMap
                )
                HomeGlanceCard(
                    modifier = Modifier.weight(1f),
                    title = "Cameras",
                    detail = when {
                        cameras.isNullOrEmpty() -> "Pair a camera"
                        else -> "${cameras.size} paired · ${if (hasOnlineCamera) "online" else "offline"}"
                    },
                    icon = Icons.Default.Visibility,
                    status = if (hasOnlineCamera) OneMint else OneAmber,
                    onClick = { showCameraSetup = true }
                )
            }
        }

        HomeRecentEvents(
            events = events,
            onOpenEvents = onOpenEvents,
            onOpenEvent = onOpenEvent
        )

        if (isBackendHome) {
            PublisherPairingCard(
                pairing = publisherPairing,
                loadState = publisherPairingLoadState,
                error = publisherPairingError,
                onCreatePairing = onCreatePublisherPairing,
                pairingStatus = cameraPairingStatus,
                pairingStatusLoadState = cameraPairingStatusLoadState,
                pairingStatusError = cameraPairingStatusError,
                onRefreshStatus = onRefreshCameraPairingStatus
            )
        }
    }

    if (showCareSpaces) {
        ModalBottomSheet(onDismissRequest = { showCareSpaces = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScreenHeader("CARE SPACES", "Choose where you’re caring.", "People, cameras, maps and consent stay scoped to one household.")
                CareSpacesCard(
                    role = OneRole.CAREGIVER,
                    isBackend = isBackendHome,
                    spaces = careSpaces,
                    loadState = careSpacesLoadState,
                    loadError = careSpacesLoadError,
                    actionState = careSpaceActionState,
                    actionError = careSpaceActionError,
                    canCreate = canCreateCareSpace,
                    onRetry = onCareSpacesRetry,
                    onCreate = onCreateCareSpace,
                    onActivate = onActivateCareSpace
                )
            }
        }
    }

    if (showCameraSetup) {
        ModalBottomSheet(onDismissRequest = { showCameraSetup = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScreenHeader("CAMERAS", "Paired views", "Camera viewing remains consent-based and receive-only for caregivers.")
                HomeCameraStatusCard(
                    paused = homeSnapshot?.profile?.paused == true,
                    cameras = cameras,
                    camerasAreStale = camerasAreStale,
                    rooms = rooms,
                    loadState = cameraLoadState,
                    loadError = cameraLoadError,
                    actionState = cameraActionState,
                    actionError = cameraActionError,
                    onRetry = onCameraRetry,
                    onRegisterCamera = onRegisterCamera,
                    onUpdateCamera = onUpdateCamera,
                    onOpenCamera = onOpenCamera,
                    videoConsentGranted = videoConsentGranted,
                    captureCameraId = captureCameraId,
                    liveKitPublishing = liveKitPublishing,
                    onStartCapture = onStartCapture,
                    onStopCapture = onStopCapture,
                    onStartLiveKit = onStartLiveKit,
                    onStopLiveKit = onStopLiveKit
                )
            }
        }
    }
}

@Composable
private fun CareSpaceContextCard(
    space: OneCareSpace?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(OneBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = OneBlue, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("CARING FOR", style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
                Text(space?.name ?: "Current care space", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    space?.let { "${it.recipientCount} people · ${it.careSetting.humanLabel()}" } ?: if (isLoading) "Loading household details…" else "Manage homes and residences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isLoading && space == null) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("⌃⌄", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeTodayCard(
    nextDose: MedicationDose?,
    eventCount: Int,
    checkInTime: String?,
    isMedicationLoading: Boolean,
    onOpenPlan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeading("TODAY", "Care overview")
                Spacer(Modifier.weight(1f))
                if (isMedicationLoading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            if (nextDose != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(OneBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = OneBlue, modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(nextDose.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (nextDose.status == DoseStatus.ACKNOWLEDGED || nextDose.status == DoseStatus.TAKEN) "Done · ${nextDose.time}" else "Due ${nextDose.time}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text("Nothing scheduled right now", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$eventCount events", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                checkInTime?.let { Text("Check-in $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextButton(onClick = onOpenPlan, contentPadding = PaddingValues(0.dp)) {
                Text("Open today’s plan", color = OneBlue, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = OneBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HomeGlanceCard(
    modifier: Modifier,
    title: String,
    detail: String,
    icon: ImageVector,
    status: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = OneBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(9.dp).background(status, CircleShape))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HomeRecentEvents(
    events: List<OneEvent>,
    onOpenEvents: () -> Unit,
    onOpenEvent: (OneEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            SectionHeading("RECENT", "Events")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenEvents, contentPadding = PaddingValues(0.dp)) { Text("See all", color = OneBlue, fontWeight = FontWeight.SemiBold) }
        }
        if (events.isEmpty()) {
            InfoCard("No recent events", "Recorded household events will appear here when ONE has them.")
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    events.take(3).forEachIndexed { index, event ->
                        if (index > 0) HorizontalDivider()
                        HomeRecentEventRow(event = event, onClick = { onOpenEvent(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRecentEventRow(event: OneEvent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).background(OneBlue.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = OneBlue, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(event.kind.label, style = MaterialTheme.typography.titleSmall)
            Text(event.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${event.location} · ${event.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(event.confidence, style = MaterialTheme.typography.labelSmall, color = if (event.confidence.startsWith("High")) OneMint else OneAmber, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeObjectsRow(homeSnapshot: OneHomeSnapshot?, isBackend: Boolean) {
    when {
        homeSnapshot != null && homeSnapshot.objects.isEmpty() -> {
            InfoCard("No objects recorded yet", "Objects appear here after a consented room setup or observation.")
        }
        homeSnapshot != null -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(homeSnapshot.objects) { remoteObject ->
                    val lastSeen = remoteObject.lastSeenAt?.let { "Last seen ${it.toHumanDateTime()}" }
                    ObjectCard(
                        title = remoteObject.label,
                        subtitle = listOfNotNull(
                            remoteObject.zone ?: if (remoteObject.status == "seen") "Home · observed" else "Home · not observed yet",
                            lastSeen
                        ).joinToString(" · "),
                        icon = Icons.Default.Visibility,
                        accent = OneCyan
                    )
                }
            }
        }
        isBackend -> InfoCard("Objects unavailable", "ONE has not received a household map snapshot yet.")
        else -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item { ObjectCard("Blue mug", "Kitchen · remembered", Icons.Default.Visibility, OneCyan) }
                item { ObjectCard("Front door", "Entry · mapped", Icons.Default.Home, OneBlue) }
                item { ObjectCard("Reading chair", "Living room", Icons.Default.Person, OneMint) }
            }
        }
    }
}

@Composable
private fun HomeCameraStatusCard(
    paused: Boolean,
    cameras: List<OneCamera>?,
    camerasAreStale: Boolean,
    rooms: List<OneRoom>?,
    loadState: OneCameraLoadState,
    loadError: String?,
    actionState: OneCameraActionState,
    actionError: String?,
    onRetry: () -> Unit,
    onRegisterCamera: (String, UUID?) -> Unit,
    onUpdateCamera: (OneCamera, String, UUID?, Boolean) -> Unit,
    onOpenCamera: (OneCamera) -> Unit,
    videoConsentGranted: Boolean,
    captureCameraId: String?,
    liveKitPublishing: Boolean,
    onStartCapture: (OneCamera) -> Unit,
    onStopCapture: () -> Unit,
    onStartLiveKit: () -> Unit,
    onStopLiveKit: () -> Unit
) {
    var showCameraDialog by rememberSaveable { mutableStateOf(false) }
    var editingCameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraName by rememberSaveable { mutableStateOf("") }
    var cameraRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraEnabled by rememberSaveable { mutableStateOf(true) }
    var cameraRoomMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val editingCamera = editingCameraId?.let { id -> cameras.orEmpty().firstOrNull { it.id.toString() == id } }
    val selectedRoomName = cameraRoomId?.let { id -> rooms.orEmpty().firstOrNull { it.id.toString() == id }?.name }
        ?: "No room assigned"
    val selectedRoomId = cameraRoomId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    var permissionRequest by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val requested = permissionRequest
        permissionRequest = null
        val cameraGranted = grants[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (requested == "capture" && cameraGranted) {
            cameras.orEmpty().firstOrNull { it.id.toString() == pendingCameraId }?.let(onStartCapture)
            pendingCameraId = null
            permissionError = null
        } else if (requested == "livekit" && cameraGranted && audioGranted) {
            onStartLiveKit()
            permissionError = null
        } else if (requested != null) {
            permissionError = "ONE needs camera permission${if (requested == "livekit") " and microphone permission" else ""} to continue."
        }
    }

    fun requestMediaPermissions(action: String, camera: OneCamera? = null) {
        if (action == "capture" && camera != null) {
            val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (cameraGranted && notificationGranted) {
                onStartCapture(camera)
                permissionError = null
            } else {
                permissionRequest = action
                pendingCameraId = camera.id.toString()
                permissionLauncher.launch((listOf(Manifest.permission.CAMERA) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()).toTypedArray())
            }
        } else {
            val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (cameraGranted && audioGranted && notificationGranted) {
                onStartLiveKit()
                permissionError = null
            } else {
                permissionRequest = action
                permissionLauncher.launch((listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()).toTypedArray())
            }
        }
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Camera and room setup", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        editingCameraId = null
                        cameraName = ""
                        cameraRoomId = null
                        cameraEnabled = true
                        showCameraDialog = true
                    },
                    enabled = actionState != OneCameraActionState.SUBMITTING
                ) { Text("Register") }
            }
            if (camerasAreStale) {
                AssistChip(onClick = onRetry, label = { Text("Offline · showing last known cameras") })
            }
            Text(
                when {
                    paused -> "Camera capture is paused until the household enables room-data consent."
                    !videoConsentGranted -> "Enable room and camera consent in Account before starting capture."
                    else -> "Camera viewing is consent-based. Live previews come from the paired device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (!videoConsentGranted && !paused) OneAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
            when {
                loadState == OneCameraLoadState.LOADING && cameras == null -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading paired cameras…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                loadState == OneCameraLoadState.ERROR && cameras == null -> {
                    Text(loadError ?: "ONE could not load the household cameras.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
                cameras.orEmpty().isEmpty() -> {
                    Text("No cameras are paired with this household yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> cameras.orEmpty().take(3).forEach { camera ->
                    CameraStatusRow(
                        camera = camera,
                        paused = paused,
                        onOpenCamera = onOpenCamera,
                        onEditCamera = {
                            editingCameraId = camera.id.toString()
                            cameraName = camera.name
                            cameraRoomId = camera.roomId?.toString()
                            cameraEnabled = camera.enabled
                            showCameraDialog = true
                        }
                    )
                }
            }
            cameras.orEmpty().firstOrNull()?.let { primaryCamera ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Capture controls", style = MaterialTheme.typography.titleSmall)
                Text("Sampling sends compressed frames to the consented vision endpoint. A paired publisher device supplies the optional real-time LiveKit feed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { if (captureCameraId == primaryCamera.id.toString()) onStopCapture() else requestMediaPermissions("capture", primaryCamera) },
                    enabled = primaryCamera.enabled && !paused && (captureCameraId == primaryCamera.id.toString() || videoConsentGranted) && !liveKitPublishing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (captureCameraId == primaryCamera.id.toString()) "Stop sampling" else "Start sampling") }
                if (liveKitPublishing) {
                    OutlinedButton(onClick = onStopLiveKit, modifier = Modifier.fillMaxWidth()) { Text("Stop LiveKit") }
                }
                if (captureCameraId != null) Text("Camera sampling is active in the foreground.", style = MaterialTheme.typography.bodySmall, color = OneMint)
                if (liveKitPublishing) Text("LiveKit publishing is active in the foreground.", style = MaterialTheme.typography.bodySmall, color = OneMint)
                permissionError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            if (actionState == OneCameraActionState.LOADED) {
                Text("Camera saved. Assigning a room helps keep the household map understandable.", style = MaterialTheme.typography.bodySmall, color = OneMint)
            }
            actionError?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showCameraDialog) {
        OneAlertDialog(
            onDismissRequest = {
                if (actionState != OneCameraActionState.SUBMITTING) showCameraDialog = false
            },
            title = { Text(if (editingCamera == null) "Register household camera" else "Edit household camera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Register the paired device in ONE. This does not publish this phone's camera or microphone.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = cameraName,
                        onValueChange = { cameraName = it.take(120) },
                        label = { Text("Camera name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(
                            onClick = { cameraRoomMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Room: $selectedRoomName") }
                        OneDropdownMenu(
                            expanded = cameraRoomMenuExpanded,
                            onDismissRequest = { cameraRoomMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No room assigned") },
                                onClick = {
                                    cameraRoomId = null
                                    cameraRoomMenuExpanded = false
                                }
                            )
                            rooms.orEmpty().forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room.name) },
                                    onClick = {
                                        cameraRoomId = room.id.toString()
                                        cameraRoomMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (editingCamera != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Camera enabled", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = cameraEnabled, onCheckedChange = { cameraEnabled = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraDialog = false
                        val camera = editingCamera
                        if (camera == null) {
                            onRegisterCamera(cameraName, selectedRoomId)
                        } else {
                            onUpdateCamera(camera, cameraName, selectedRoomId, cameraEnabled)
                        }
                        editingCameraId = null
                    },
                    enabled = cameraName.trim().isNotBlank() && actionState != OneCameraActionState.SUBMITTING
                ) { Text(if (actionState == OneCameraActionState.SUBMITTING) "Saving…" else if (editingCamera == null) "Register" else "Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCameraDialog = false
                        editingCameraId = null
                    },
                    enabled = actionState != OneCameraActionState.SUBMITTING
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PublisherPairingCard(
    pairing: PublisherPairingStartResponse?,
    loadState: OneFamilyInviteLoadState,
    error: String?,
    onCreatePairing: (String) -> Unit,
    pairingStatus: OneCameraPairingStatus?,
    pairingStatusLoadState: OneCameraPairingStatusLoadState,
    pairingStatusError: String?,
    onRefreshStatus: () -> Unit
) {
    var deviceLabel by rememberSaveable { mutableStateOf("ONE room device") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pair a publisher device", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use this one-time code on the Android phone that will publish the consented camera and microphone feed. Publisher devices cannot access family or household controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = deviceLabel,
                onValueChange = { deviceLabel = it.take(120) },
                label = { Text("Device label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onCreatePairing(deviceLabel) },
                enabled = deviceLabel.trim().isNotBlank() && loadState != OneFamilyInviteLoadState.SUBMITTING,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loadState == OneFamilyInviteLoadState.SUBMITTING) "Creating code…" else "Create pairing code")
            }
            pairing?.let {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Publisher pairing code ${it.pairingCode}. Expires in ${it.expiresInSeconds / 60} minutes."
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("ONE-TIME PAIRING CODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(it.pairingCode, style = MaterialTheme.typography.headlineMedium, letterSpacing = 4.sp)
                        Text("Expires in about ${it.expiresInSeconds / 60} minutes. Share it only with the intended device.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            pairingStatus?.let { status ->
                val statusTint = when (status.status) {
                    "connected" -> OneMint
                    "expired" -> OneAmber
                    else -> OneBlue
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = statusTint.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pairing status", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(status.status.humanLabel(), style = MaterialTheme.typography.labelMedium, color = statusTint, fontWeight = FontWeight.Bold)
                        }
                        status.deviceLabel?.let { label -> Text("Device: $label", style = MaterialTheme.typography.bodySmall) }
                        status.connectedAt?.let { connectedAt -> Text("Connected ${connectedAt.toHumanDateTime()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        status.expiresAt?.let { expiresAt ->
                            if (status.status != "connected") Text("Code expires ${expiresAt.toHumanDateTime()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = onRefreshStatus,
                            enabled = pairingStatusLoadState != OneCameraPairingStatusLoadState.LOADING,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (pairingStatusLoadState == OneCameraPairingStatusLoadState.LOADING) "Refreshing…" else "Refresh status") }
                    }
                }
            }
            pairingStatusError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun PublisherScreen(
    isPublishing: Boolean,
    hasReconnectLink: Boolean,
    reconnectLoadState: OneCameraReconnectLoadState,
    reconnectError: String?,
    onCreateReconnectLink: () -> Unit,
    onReconnect: () -> Unit,
    onSignOut: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    var explicitMediaConsent by rememberSaveable { mutableStateOf(false) }
    var permissionError by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            permissionError = null
            onStart()
        } else {
            permissionError = "Camera and microphone permissions are required to publish this device."
        }
    }
    fun startPublisher() {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            permissionError = null
            onStart()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }
    ScreenScroll {
        ScreenHeader("PUBLISHER", "This phone is a room device.", "It only publishes the camera and microphone feed allowed by the household.")
        InfoCard("Publisher permissions", "A caregiver must enable video and audio consent for the represented person before the feed can be used. This device has no access to family, medication or household controls.")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reconnect protection", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (hasReconnectLink) "A protected reconnect link is stored on this device. Use it after a process restart if the live feed needs to be recovered."
                    else "Create a protected reconnect link so this paired camera can recover after a process restart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCreateReconnectLink,
                        enabled = reconnectLoadState != OneCameraReconnectLoadState.SUBMITTING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                reconnectLoadState == OneCameraReconnectLoadState.SUBMITTING -> "Saving…"
                                hasReconnectLink -> "Refresh link"
                                else -> "Create link"
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = onReconnect,
                        enabled = hasReconnectLink && reconnectLoadState != OneCameraReconnectLoadState.SUBMITTING,
                        modifier = Modifier.weight(1f)
                    ) { Text("Reconnect") }
                }
                if (reconnectLoadState == OneCameraReconnectLoadState.LOADED) {
                    Text("Reconnect link updated.", style = MaterialTheme.typography.bodySmall, color = OneMint)
                }
                reconnectError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
        ConsentRow(
            label = "I understand this device will publish camera and microphone while active.",
            enabled = explicitMediaConsent,
            onChanged = { explicitMediaConsent = it }
        )
        Button(
            onClick = { if (isPublishing) onStop() else startPublisher() },
            enabled = isPublishing || explicitMediaConsent,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isPublishing) MaterialTheme.colorScheme.error else OneBlue)
        ) {
            Icon(if (isPublishing) Icons.Default.Visibility else Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text(if (isPublishing) "Stop publishing" else "Start publishing", style = MaterialTheme.typography.titleMedium)
        }
        if (isPublishing) {
            Text("Publishing is active in the foreground. Android will show an ongoing notification.", style = MaterialTheme.typography.bodySmall, color = OneMint)
        }
        permissionError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Text("The caregiver viewer is receive-only. This device publishes camera and microphone only while you keep publishing active.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }
    }
}

@Composable
private fun CameraStatusRow(
    camera: OneCamera,
    paused: Boolean,
    onOpenCamera: (OneCamera) -> Unit,
    onEditCamera: (() -> Unit)? = null
) {
    val status = if (paused) "Paused" else camera.status.cameraStatusLabel(camera.enabled, camera.platform)
    val tint = if (paused) OneAmber else camera.status.cameraStatusTint(camera.enabled, camera.platform)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(enabled = camera.enabled && !paused, role = Role.Button) { onOpenCamera(camera) }
            .semantics(mergeDescendants = true) { contentDescription = "Open camera ${camera.name}. Status: $status" }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Visibility, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(camera.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${camera.roomId?.let { "Room configured" } ?: "Room not assigned"} · ${camera.platform.takeUnless { it.equals("browser", true) } ?: "paired device"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.size(9.dp).background(tint, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(status, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
        onEditCamera?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Edit, contentDescription = "Edit camera", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        if (camera.enabled && !paused) {
            Spacer(Modifier.width(7.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open camera", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

private fun String.cameraStatusLabel(enabled: Boolean, platform: String = ""): String = when {
    !enabled -> "Offline"
    platform.equals("browser", ignoreCase = true) && equals("online", ignoreCase = true) -> "Registered"
    equals("online", ignoreCase = true) -> "Online"
    equals("paused", ignoreCase = true) -> "Paused"
    equals("offline", ignoreCase = true) -> "Offline"
    else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun String.cameraStatusTint(enabled: Boolean, platform: String = ""): Color = when {
    !enabled || equals("offline", ignoreCase = true) -> OneAmber
    equals("online", ignoreCase = true) -> OneMint
    equals("paused", ignoreCase = true) -> OneAmber
    else -> OneCyan
}

@Composable
private fun LiveCameraScreen(
    camera: OneCamera,
    apiClient: OneApiClient,
    session: OneSession?,
    onClose: () -> Unit
) {
    var liveToken by remember(camera.id, session?.accessToken) { mutableStateOf<OneLiveKitToken?>(null) }
    var tokenError by remember(camera.id, session?.accessToken) { mutableStateOf<String?>(null) }

    LaunchedEffect(camera.id, session?.accessToken) {
        val authenticatedSession = session
        if (authenticatedSession == null) {
            tokenError = "A signed-in caregiver session is required to view this camera."
        } else {
            runCatching { apiClient.liveKitToken(authenticatedSession, mode = "subscribe") }
                .onSuccess { liveToken = it }
                .onFailure { tokenError = it.message ?: "ONE could not start the live camera view." }
        }
    }

    ScreenScroll {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp).offset(x = (-3).dp).rotate(180f))
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }
        }
        ScreenHeader("CAMERA", camera.name, "Consent-based live view · receive only")
        when {
            tokenError != null -> {
                InfoCard("Live view unavailable", tokenError ?: "ONE could not start the live camera view.")
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
            liveToken == null -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Requesting a short-lived viewer token…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LiveKitCameraSurface(liveToken!!)
        }
        InfoCard("Viewer preview", "This LiveKit subscriber is a receive-only preview. Verify the deployed LiveKit subscriber and retention controls before relying on it in production.")
        InfoCard("Privacy reminder", "This view is receive-only. ONE does not publish this device's camera or microphone, and the room disconnects when you leave.")
    }
}

@Composable
private fun LiveKitCameraSurface(token: OneLiveKitToken) {
    var roomError by remember(token.participantToken) { mutableStateOf<String?>(null) }
    RoomScope(
        url = token.serverUrl,
        token = token.participantToken,
        audio = false,
        video = false,
        connect = true,
        onError = { _, error -> roomError = error?.message ?: "The live camera connection failed." }
    ) { room ->
        val trackRefs by rememberTracks()
        val cameraTrack = trackRefs.firstOrNull { track -> track.source == Track.Source.CAMERA && track.isSubscribed() }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(OneInverseSurface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    roomError != null -> Text(roomError ?: "Live camera connection failed.", modifier = Modifier.padding(24.dp), color = Color.White)
                    cameraTrack != null -> VideoTrackView(trackReference = cameraTrack, modifier = Modifier.fillMaxSize(), room = room)
                    else -> Text("Waiting for the camera stream…", color = Color.White.copy(alpha = 0.85f))
                }
                Row(modifier = Modifier.align(Alignment.TopStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(OneCyan, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE · RECEIVE ONLY", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text("The camera stream is supplied by the paired household device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CameraHeroCard(isDemo: Boolean) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = OneInverseSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
                .background(
                    Brush.linearGradient(listOf(OneInverseSurface, OneBlue.copy(alpha = 0.85f))),
                    RoundedCornerShape(30.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (isDemo) "DEMO ROOM PREVIEW" else "LIVING ROOM CAMERA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isDemo) "SAMPLE" else "LIVE", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(76.dp).align(Alignment.CenterHorizontally))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDemo) "Illustrative preview · no camera connected" else "A steady view of the room", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ObjectCard(title: String, subtitle: String, icon: ImageVector, accent: Color) {
    Card(
        modifier = Modifier
            .width(188.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), MaterialTheme.colorScheme.background))),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onBackground) }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HouseholdStatusCard(
    homeSnapshot: OneHomeSnapshot?,
    isDemo: Boolean,
    onOpenStatus: () -> Unit,
    onOpenPlan: () -> Unit
) {
    val status = when {
        isDemo -> "Demo preview · sample data"
        homeSnapshot == null -> "No household snapshot"
        homeSnapshot.profile.paused -> "Camera capture paused"
        else -> "${homeSnapshot.objects.size} objects · ${homeSnapshot.events.size} events"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
            StatusRow("Household status", status, Icons.Default.CheckCircle, OneMint, onOpenStatus)
            HorizontalDivider()
            StatusRow(
                "This week's plan",
                if (isDemo) "Sample check-ins · illustrative" else "${homeSnapshot?.events?.size ?: 0} recent events",
                Icons.Default.Schedule,
                OneBlue,
                onOpenPlan
            )
        }
    }
}

@Composable
private fun StatusRow(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
private fun FamilyScreen(
    members: List<OneFamilyMember>?,
    currentUserId: UUID?,
    isBackend: Boolean,
    loadState: OneFamilyLoadState,
    loadError: String?,
    onRetry: () -> Unit,
    canInvite: Boolean,
    familyInviteLoadState: OneFamilyInviteLoadState,
    familyInvite: OneFamilyInvite?,
    familyInviteLoadError: String?,
    onCreateInvite: (FamilyInviteRequest) -> Unit,
    careRecipients: List<OneCareRecipient>?,
    careRecipientsLoadState: OneCareRecipientLoadState,
    careRecipientsLoadError: String?,
    careRecipientActionState: OneCareRecipientActionState,
    careRecipientActionError: String?,
    canManageRecipients: Boolean,
    onCareRecipientsRetry: () -> Unit,
    onCreateCareRecipient: (String, String?, String?) -> Unit,
    onUpdateCareRecipient: (OneCareRecipient, String, String?, String?) -> Unit,
    onDeleteCareRecipient: (OneCareRecipient) -> Unit,
    familyMemberActionState: OneFamilyMemberActionState,
    familyMemberActionError: String?,
    familyMemberActionId: UUID?,
    isAdmin: Boolean,
    onUpdateFamilyMember: (OneFamilyMember, OneRole) -> Unit,
    onRemoveFamilyMember: (OneFamilyMember) -> Unit,
    selectedFamilySubjectId: UUID?,
    selectedSubjectMedicationConsent: Boolean,
    selectedSubjectFamilyConsent: Boolean,
    selectedSubjectAssistantConsent: Boolean,
    onSelectFamilySubject: (UUID) -> Unit,
    medicationDoses: List<MedicationDose>?,
    medicationLoadState: OneMedicationLoadState,
    medicationLoadError: String?,
    onMedicationRetry: () -> Unit,
    medicationPlans: List<OneMedicationPlan>?,
    medicationPlansLoadState: OneMedicationLoadState,
    medicationPlansLoadError: String?,
    onMedicationPlansRetry: () -> Unit,
    medicationCheckIns: List<OneRemoteMedicationCheckIn>?,
    medicationCheckInsLoadState: OneMedicationLoadState,
    medicationCheckInsLoadError: String?,
    medicationHistoryDays: Int,
    onMedicationHistoryRetry: () -> Unit,
    onMedicationHistoryRangeChange: (Int) -> Unit,
    medicationPlanActionState: OneMedicationPlanActionState,
    lastMedicationPlan: OneMedicationPlan?,
    medicationPlanActionError: String?,
    onCreateMedicationPlan: (String, String, String, String, UUID?) -> Unit,
    onUpdateMedicationPlan: (OneMedicationPlan, String, String, String, String, Boolean, UUID?) -> Unit,
    medicationActionKey: String?,
    medicationActionError: String?,
    onMedicationStatusChange: (MedicationDose, DoseStatus) -> Unit
) {
    var showInviteDialog by rememberSaveable { mutableStateOf(false) }
    var inviteName by rememberSaveable { mutableStateOf("") }
    var inviteEmail by rememberSaveable { mutableStateOf("") }
    var inviteRoleName by rememberSaveable { mutableStateOf(OneRole.CAREGIVER.name) }
    val inviteRole = if (inviteRoleName == OneRole.RESIDENT.name) OneRole.RESIDENT else OneRole.CAREGIVER
    val canSubmitInvite = inviteName.trim().isNotBlank() && familyInviteLoadState != OneFamilyInviteLoadState.SUBMITTING
    var subjectMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showMedicationPlanDialog by rememberSaveable { mutableStateOf(false) }
    var editingMedicationPlanId by rememberSaveable { mutableStateOf<String?>(null) }
    var planName by rememberSaveable { mutableStateOf("") }
    var planDose by rememberSaveable { mutableStateOf("") }
    var planSchedule by rememberSaveable { mutableStateOf("08:00") }
    var planInstructions by rememberSaveable { mutableStateOf("") }
    var planAssignedCaregiverId by rememberSaveable { mutableStateOf<String?>(null) }
    var assignedCaregiverMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var medicationHistoryStatus by rememberSaveable { mutableStateOf("all") }
    val selectedSubjectName = members?.firstOrNull { it.id == selectedFamilySubjectId }?.displayName
        ?: if (isBackend) "My view" else "Everyone"
    val editingMedicationPlan = editingMedicationPlanId?.let { id -> medicationPlans.orEmpty().firstOrNull { it.id.toString() == id } }
    val caregiverMembers = members.orEmpty().filter { it.role.equals("admin", ignoreCase = true) || it.role.equals("caregiver", ignoreCase = true) }
    val assignedCaregiverName = planAssignedCaregiverId?.let { id -> caregiverMembers.firstOrNull { it.id.toString() == id }?.displayName }
    val medicationAccessGranted = !isBackend || selectedSubjectMedicationConsent
    val familyAccessGranted = !isBackend || selectedSubjectFamilyConsent
    val assistantAccessGranted = !isBackend || selectedSubjectAssistantConsent
    val visibleMedicationDoses = if (isBackend) {
        medicationDoses.orEmpty().filter(MedicationDose::isOpenReminder)
    } else {
        demoMedicationDoses.filter(MedicationDose::isOpenReminder)
    }
    val canCreateMedicationPlan = isBackend && selectedFamilySubjectId != null && medicationAccessGranted && medicationPlanActionState != OneMedicationPlanActionState.SUBMITTING
    val canSubmitMedicationPlan = planName.trim().isNotBlank() && planDose.trim().isNotBlank() && planSchedule.trim().isNotBlank() && medicationPlanActionState != OneMedicationPlanActionState.SUBMITTING

    ScreenScroll {
        ScreenHeader("CARE CIRCLE", "Family", "People, reminders, and permissions around the home.")
        if (!isBackend) {
            AssistChip(onClick = { }, enabled = false, label = { Text("Demo preview · synthetic family data") })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssistChip(
                    onClick = { subjectMenuExpanded = true },
                    enabled = isBackend && !members.isNullOrEmpty(),
                    label = { Text(selectedSubjectName) },
                    leadingIcon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                OneDropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    members.orEmpty().forEach { member ->
                        DropdownMenuItem(
                            text = { Text("${member.displayName} · ${member.familyRoleLabel()}") },
                            onClick = {
                                subjectMenuExpanded = false
                                onSelectFamilySubject(member.id)
                            }
                        )
                    }
                }
            }
        }
        Text("Showing plans and observations for $selectedSubjectName. Switch people before reviewing sensitive details.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isBackend && selectedFamilySubjectId != null) {
            if (!familyAccessGranted) InfoCard("Family sharing is paused", "An active family_mode consent for $selectedSubjectName is required before reviewing family details.")
            if (!medicationAccessGranted) InfoCard("Medication controls are paused", "An active medication_management consent for $selectedSubjectName is required. The information below stays administrative and non-medical.")
            if (!assistantAccessGranted) InfoCard("Family assistant is paused", "An active family_assistant consent for $selectedSubjectName is required before sending a bounded summary request.")
        }
        SectionHeading("CARE RECIPIENTS", "People receiving support")
        CareRecipientsCard(
            isBackend = isBackend,
            recipients = careRecipients,
            loadState = careRecipientsLoadState,
            loadError = careRecipientsLoadError,
            actionState = careRecipientActionState,
            actionError = careRecipientActionError,
            canManage = canManageRecipients,
            onRetry = onCareRecipientsRetry,
            onCreate = onCreateCareRecipient,
            onUpdate = onUpdateCareRecipient,
            onDelete = onDeleteCareRecipient
        )
        SectionHeading("TODAY'S PLAN", "Medication reminders")
        if (canCreateMedicationPlan) {
            OutlinedButton(
                onClick = {
                    editingMedicationPlanId = null
                    planName = ""
                    planDose = ""
                    planSchedule = "08:00"
                    planInstructions = ""
                    planAssignedCaregiverId = null
                    showMedicationPlanDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add medication plan for $selectedSubjectName") }
        }
        medicationPlanActionError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        lastMedicationPlan?.let { plan ->
            InfoCard(
                "Medication plan saved",
                "${plan.name} · ${plan.dose} · ${plan.schedule}. Reminders are administrative only; confirm decisions with the resident and care team."
            )
        }
        if (isBackend && canCreateMedicationPlan) {
            when {
                medicationPlans == null && (medicationPlansLoadState == OneMedicationLoadState.IDLE || medicationPlansLoadState == OneMedicationLoadState.LOADING) -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading active medication plans…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                medicationPlans == null && medicationPlansLoadState == OneMedicationLoadState.ERROR -> {
                    InfoCard("Medication plans unavailable", medicationPlansLoadError ?: "ONE could not load active medication plans.")
                    OutlinedButton(onClick = onMedicationPlansRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry plan loading") }
                }
                medicationPlans.isNullOrEmpty() -> {
                    InfoCard("No active plans", "Create a plan above to schedule administrative reminders for this person.")
                }
                else -> {
                    medicationPlans.orEmpty().forEach { plan ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = OneBlue, modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(plan.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${plan.dose} · ${plan.schedule}", style = MaterialTheme.typography.bodyMedium)
                                        if (plan.instructions.isNotBlank()) {
                                            Text(plan.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            plan.assignedCaregiverId?.let { caregiverId ->
                                                "Assigned caregiver: ${caregiverMembers.firstOrNull { it.id == caregiverId }?.displayName ?: "Caregiver"}"
                                            } ?: "No caregiver assigned",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = OneMint.copy(alpha = 0.12f)) {
                                        Text("ACTIVE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = OneMint, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            editingMedicationPlanId = plan.id.toString()
                                            planName = plan.name
                                            planDose = plan.dose
                                            planSchedule = plan.schedule
                                            planInstructions = plan.instructions
                                            planAssignedCaregiverId = plan.assignedCaregiverId?.toString()
                                            showMedicationPlanDialog = true
                                        },
                                        enabled = medicationPlanActionState != OneMedicationPlanActionState.SUBMITTING,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Edit") }
                                    TextButton(
                                        onClick = {
                                            onUpdateMedicationPlan(plan, plan.name, plan.dose, plan.schedule, plan.instructions, false, plan.assignedCaregiverId)
                                        },
                                        enabled = medicationPlanActionState != OneMedicationPlanActionState.SUBMITTING,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Deactivate") }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!isBackend) {
            if (visibleMedicationDoses.isEmpty()) {
                InfoCard("No open reminders", "Today's completed reminders are available in the history section.")
            } else {
                visibleMedicationDoses.forEach { dose -> MedicationRow(dose) }
            }
        } else when {
            medicationDoses == null && (medicationLoadState == OneMedicationLoadState.IDLE || medicationLoadState == OneMedicationLoadState.LOADING) -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading medication reminders…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            medicationDoses == null && medicationLoadState == OneMedicationLoadState.ERROR -> {
                InfoCard("Medication data unavailable", medicationLoadError ?: "ONE could not load medication reminders.")
                OutlinedButton(onClick = onMedicationRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            visibleMedicationDoses.isEmpty() -> InfoCard(
                if (medicationDoses.isNullOrEmpty()) "No reminders for today" else "No open reminders",
                if (medicationDoses.isNullOrEmpty()) {
                    "No active medication reminder has been scheduled for this household today."
                } else {
                    "Today's completed reminders are available in the history section."
                }
            )
            else -> {
                medicationActionError?.let { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                visibleMedicationDoses.forEach { dose ->
                    MedicationRow(
                        dose = dose,
                        actionKey = medicationActionKey,
                        onStatusChange = if (medicationAccessGranted) ({ status -> onMedicationStatusChange(dose, status) }) else null
                    )
                }
            }
        }
        Text("Reminders support organization only. Confirm medication decisions with the resident and their care team.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionHeading("HISTORY", "Medication check-ins")
        if (!isBackend) {
            InfoCard("Backend-only history", "Connect a backend to review recorded medication check-ins by date and status.")
        } else {
            val historyFilters = listOf("all" to "All", "pending" to "Pending", "taken" to "Taken", "skipped" to "Skipped", "missed" to "Missed")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyFilters) { (value, label) ->
                    FilterChip(
                        selected = medicationHistoryStatus == value,
                        onClick = { medicationHistoryStatus = value },
                        label = { Text(label) }
                    )
                }
            }
            Text("Showing the last ${countLabel(medicationHistoryDays, "day")} plus upcoming check-ins.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                medicationCheckIns == null && (medicationCheckInsLoadState == OneMedicationLoadState.IDLE || medicationCheckInsLoadState == OneMedicationLoadState.LOADING) -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading check-in history…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                medicationCheckIns == null && medicationCheckInsLoadState == OneMedicationLoadState.ERROR -> {
                    InfoCard("History unavailable", medicationCheckInsLoadError ?: "ONE could not load medication check-ins.")
                    OutlinedButton(onClick = onMedicationHistoryRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry history") }
                }
                else -> {
                    val visibleCheckIns = medicationCheckIns.orEmpty().filter { checkIn ->
                        medicationHistoryStatus == "all" || checkIn.status.equals(medicationHistoryStatus, ignoreCase = true)
                    }
                    if (visibleCheckIns.isEmpty()) {
                        InfoCard("No matching check-ins", "No medication check-ins match this period and status.")
                    } else {
                        visibleCheckIns.take(30).forEach { checkIn ->
                            val planLabel = medicationPlans.orEmpty().firstOrNull { it.id == checkIn.planId }?.name ?: "Medication plan"
                            val statusTint = if (checkIn.status.equals("taken", ignoreCase = true)) OneMint else OneAmber
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(planLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        Text(checkIn.status.humanLabel(), style = MaterialTheme.typography.labelSmall, color = statusTint, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(checkIn.scheduledFor.toHumanDateTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    checkIn.note?.takeIf { it.isNotBlank() }?.let { note ->
                                        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FamilyMembersSection(
            members = members,
            currentUserId = currentUserId,
            isBackend = isBackend,
            loadState = loadState,
            loadError = loadError,
            onRetry = onRetry,
            canInvite = canInvite,
            onInvite = { showInviteDialog = true },
            familyInvite = familyInvite,
            familyInviteLoadError = familyInviteLoadError,
            familyMemberActionState = familyMemberActionState,
            familyMemberActionError = familyMemberActionError,
            familyMemberActionId = familyMemberActionId,
            isAdmin = isAdmin,
            onUpdateFamilyMember = onUpdateFamilyMember,
            onRemoveFamilyMember = onRemoveFamilyMember
        )

    }
    if (showInviteDialog) {
        OneAlertDialog(
            onDismissRequest = {
                if (familyInviteLoadState != OneFamilyInviteLoadState.SUBMITTING) showInviteDialog = false
            },
            title = { Text("Invite someone to the care circle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a one-time code to share with a trusted person.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = inviteName,
                        onValueChange = { inviteName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Role", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = inviteRole == OneRole.CAREGIVER,
                            onClick = { inviteRoleName = OneRole.CAREGIVER.name },
                            label = { Text("Caregiver") }
                        )
                        FilterChip(
                            selected = inviteRole == OneRole.RESIDENT,
                            onClick = { inviteRoleName = OneRole.RESIDENT.name },
                            label = { Text("Resident") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInviteDialog = false
                        onCreateInvite(
                            FamilyInviteRequest(
                                displayName = inviteName,
                                email = inviteEmail,
                                role = inviteRole
                            )
                        )
                    },
                    enabled = canSubmitInvite
                ) {
                    Text(if (familyInviteLoadState == OneFamilyInviteLoadState.SUBMITTING) "Creating…" else "Create invite")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInviteDialog = false },
                    enabled = familyInviteLoadState != OneFamilyInviteLoadState.SUBMITTING
                ) { Text("Cancel") }
            }
        )
    }
    if (showMedicationPlanDialog) {
        OneAlertDialog(
            onDismissRequest = {
                if (medicationPlanActionState != OneMedicationPlanActionState.SUBMITTING) {
                    showMedicationPlanDialog = false
                    editingMedicationPlanId = null
                }
            },
            title = {
                Text(
                    if (editingMedicationPlan == null) "Add a medication plan" else "Edit medication plan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Enter the plan exactly as provided by the resident's care team.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        label = { Text("Medication name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = planDose,
                        onValueChange = { planDose = it },
                        label = { Text("Dose") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = planSchedule,
                        onValueChange = { planSchedule = it },
                        label = { Text("Schedule") },
                        placeholder = { Text("08:00 or Mon,Wed,Fri @ 08:00") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = planInstructions,
                        onValueChange = { planInstructions = it },
                        label = { Text("Instructions (optional)") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isBackend) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { assignedCaregiverMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Assigned caregiver: ${assignedCaregiverName ?: "None"}")
                            }
                            OneDropdownMenu(
                                expanded = assignedCaregiverMenuExpanded,
                                onDismissRequest = { assignedCaregiverMenuExpanded = false },
                                menuWidth = maxWidth
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "No caregiver assigned",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            color = if (planAssignedCaregiverId == null) OneBlue else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (planAssignedCaregiverId == null) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (planAssignedCaregiverId == null) OneBlue.copy(alpha = 0.10f) else Color.Transparent),
                                    onClick = {
                                        planAssignedCaregiverId = null
                                        assignedCaregiverMenuExpanded = false
                                    }
                                )
                                caregiverMembers.forEach { caregiver ->
                                    val isSelected = planAssignedCaregiverId == caregiver.id.toString()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                caregiver.displayName,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                color = if (isSelected) OneBlue else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) OneBlue.copy(alpha = 0.10f) else Color.Transparent),
                                        onClick = {
                                            planAssignedCaregiverId = caregiver.id.toString()
                                            assignedCaregiverMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (medicationPlanActionError != null) {
                        Text(medicationPlanActionError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMedicationPlanDialog = false
                        val plan = editingMedicationPlan
                        if (plan == null) {
                            onCreateMedicationPlan(
                                planName,
                                planDose,
                                planSchedule,
                                planInstructions,
                                planAssignedCaregiverId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                            )
                        } else {
                            onUpdateMedicationPlan(
                                plan,
                                planName,
                                planDose,
                                planSchedule,
                                planInstructions,
                                true,
                                planAssignedCaregiverId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                            )
                        }
                        editingMedicationPlanId = null
                    },
                    enabled = canSubmitMedicationPlan,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OneBlue)
                ) { Text(if (medicationPlanActionState == OneMedicationPlanActionState.SUBMITTING) "Saving…" else if (editingMedicationPlan == null) "Save plan" else "Save changes") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMedicationPlanDialog = false
                        editingMedicationPlanId = null
                    },
                    enabled = medicationPlanActionState != OneMedicationPlanActionState.SUBMITTING
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FamilyMembersSection(
    members: List<OneFamilyMember>?,
    currentUserId: UUID?,
    isBackend: Boolean,
    loadState: OneFamilyLoadState,
    loadError: String?,
    onRetry: () -> Unit,
    canInvite: Boolean,
    onInvite: () -> Unit,
    familyInvite: OneFamilyInvite?,
    familyInviteLoadError: String?,
    familyMemberActionState: OneFamilyMemberActionState,
    familyMemberActionError: String?,
    familyMemberActionId: UUID?,
    isAdmin: Boolean,
    onUpdateFamilyMember: (OneFamilyMember, OneRole) -> Unit,
    onRemoveFamilyMember: (OneFamilyMember) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeading("ACCESS", "People with access")
            Spacer(Modifier.weight(1f))
            if (canInvite) {
                OutlinedButton(onClick = onInvite) { Text("Invite") }
            }
        }
        when {
            !isBackend -> DemoFamilyMembersCard()
            members == null && loadState in setOf(OneFamilyLoadState.IDLE, OneFamilyLoadState.LOADING) -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading the care circle…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            members == null && loadState == OneFamilyLoadState.ERROR -> {
                InfoCard("Family data unavailable", loadError ?: "ONE could not load the care circle.")
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            members.isNullOrEmpty() -> InfoCard("No family members recorded yet", "Invite a trusted person from the care circle when family sharing is enabled.")
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        members.forEachIndexed { index, member ->
                            if (index > 0) HorizontalDivider()
                            val isCurrentUser = member.id == currentUserId
                            CaregiverRow(
                                name = member.displayName,
                                relationship = null,
                                role = member.familyRoleLabel(),
                                tint = member.familyTint(),
                                isCurrentUser = isCurrentUser,
                                actions = if (isBackend && canInvite && !isCurrentUser && !member.role.equals("admin", ignoreCase = true)) {
                                    {
                                        FamilyMemberAccessActions(
                                            member = member,
                                            isBusy = familyMemberActionState == OneFamilyMemberActionState.SUBMITTING && familyMemberActionId == member.id,
                                            canPromote = isAdmin,
                                            onUpdate = onUpdateFamilyMember,
                                            onRemove = onRemoveFamilyMember
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }
        familyMemberActionError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        familyInviteLoadError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        familyInvite?.let { invite ->
            val roleLabel = if (invite.role.equals("resident", ignoreCase = true)) "Resident" else "Caregiver"
            InfoCard(
                "Invitation ready",
                "Share this one-time code with the invited person. Role: $roleLabel · expires in ${invite.expiresInSeconds / 3600}h."
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = OneBlue.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ONE-TIME INVITE CODE", style = MaterialTheme.typography.labelSmall, color = OneBlue, fontWeight = FontWeight.Bold)
                    Text(invite.code, style = MaterialTheme.typography.headlineMedium, color = OneBlue, fontWeight = FontWeight.Bold)
                    Text("The code is shown only on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CaregiverAssistantScreen(
    isBackend: Boolean,
    members: List<OneFamilyMember>?,
    selectedFamilySubjectId: UUID?,
    selectedSubjectAssistantConsent: Boolean,
    familyLoadState: OneFamilyLoadState,
    familyLoadError: String?,
    onFamilyRetry: () -> Unit,
    onSelectFamilySubject: (UUID) -> Unit,
    familyAssistantLoadState: OneFamilyAssistantLoadState,
    familyAssistantResult: OneFamilyAssistantResult?,
    familyAssistantLoadError: String?,
    onFamilyAssistantSubmit: (String) -> Unit
) {
    var subjectMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var questionHistory by rememberSaveable { mutableStateOf(listOf<String>()) }
    val selectedSubjectName = members?.firstOrNull { it.id == selectedFamilySubjectId }?.displayName
        ?: if (isBackend) "My view" else "Everyone"
    val canSend = isBackend && selectedFamilySubjectId != null && selectedSubjectAssistantConsent &&
        draft.trim().isNotBlank() && familyAssistantLoadState != OneFamilyAssistantLoadState.SUBMITTING

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenHeader(
                    "ASSISTANT",
                    "A calmer view for the care team.",
                    "Summaries from recorded medication plans and check-ins only."
                )
            }
            if (isBackend && members == null && familyLoadState in setOf(OneFamilyLoadState.IDLE, OneFamilyLoadState.LOADING)) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading the people in this care space…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isBackend && members == null && familyLoadState == OneFamilyLoadState.ERROR) {
                item {
                    InfoCard("Care circle unavailable", familyLoadError ?: "ONE could not load the people represented by this space.")
                    OutlinedButton(onClick = onFamilyRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
            }
            if (!isBackend) {
                item { AssistChip(onClick = { }, enabled = false, label = { Text("Demo preview · illustrative assistant") }) }
            }
            if (members.orEmpty().isNotEmpty()) {
                item {
                    Box {
                        AssistChip(
                            onClick = { subjectMenuExpanded = true },
                            label = { Text("For $selectedSubjectName") },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) }
                        )
                        OneDropdownMenu(
                            expanded = subjectMenuExpanded,
                            onDismissRequest = { subjectMenuExpanded = false }
                        ) {
                            members.orEmpty().forEach { member ->
                                DropdownMenuItem(
                                    text = { Text("${member.displayName} · ${member.familyRoleLabel()}") },
                                    onClick = {
                                        subjectMenuExpanded = false
                                        onSelectFamilySubject(member.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                InfoCard(
                    "Bounded family assistant",
                    "I can summarize the selected person's active medication plans and recorded check-ins. I do not make care or medication decisions."
                )
            }
            if (isBackend && selectedFamilySubjectId != null && !selectedSubjectAssistantConsent) {
                item {
                    InfoCard(
                        "Family assistant is paused",
                        "An active family_assistant consent for $selectedSubjectName is required before sending a summary request."
                    )
                }
            }
            items(questionHistory) { question ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        question,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    )
                }
            }
            familyAssistantResult?.let { result ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Summary for $selectedSubjectName", style = MaterialTheme.typography.titleMedium)
                            Text(result.summary, style = MaterialTheme.typography.bodyLarge)
                            Text("Next action: ${result.nextAction}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Limitations: ${result.limitations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (result.evidenceIds.isNotEmpty()) {
                                Text("Bounded evidence: ${countLabel(result.evidenceIds.size, "record")}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (result.degraded) {
                                Text("This response used a limited local fallback.", style = MaterialTheme.typography.bodySmall, color = OneAmber)
                            }
                        }
                    }
                }
            }
            familyAssistantLoadError?.let { error ->
                item { Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(1_000) },
                    modifier = Modifier.weight(1f),
                    enabled = isBackend,
                    placeholder = { Text("Ask about the recorded plan") },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        val message = draft.trim()
                        if (!canSend) return@IconButton
                        questionHistory = (questionHistory + message).takeLast(20)
                        draft = ""
                        onFamilyAssistantSubmit(message)
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (canSend) OneBlue else OneBlue.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DemoFamilyMembersCard() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            CaregiverRow("Biel Martínez", null, "Owner", OneBlue, isCurrentUser = true)
            HorizontalDivider()
            CaregiverRow("Marta Martínez", "Daughter", "Primary caregiver", OneBlue)
            HorizontalDivider()
            CaregiverRow("Joan Soler", "Neighbour", "Supporter", OneCyan)
        }
    }
}

private fun OneFamilyMember.familyRoleLabel(): String = when (role.lowercase()) {
    "admin" -> "Owner"
    "caregiver" -> "Caregiver"
    "resident" -> "Resident"
    else -> role.replaceFirstChar { it.uppercase() }
}

private fun OneFamilyMember.familyTint(): Color = if (role.equals("resident", ignoreCase = true)) OneCyan else OneBlue

@Composable
private fun CaregiverRow(
    name: String,
    relationship: String?,
    role: String,
    tint: Color,
    isCurrentUser: Boolean = false,
    actions: (@Composable () -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp).semantics(mergeDescendants = true) { contentDescription = "$name.${if (isCurrentUser) " You." else ""}${relationship?.let { " $it." } ?: ""} Role: $role" }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(tint.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text(name.first().toString(), style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                if (isCurrentUser) {
                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
                }
            }
            relationship?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.12f)) {
            Text(role, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
        }
        actions?.invoke()
    }
}

@Composable
private fun FamilyMemberAccessActions(
    member: OneFamilyMember,
    isBusy: Boolean,
    canPromote: Boolean,
    onUpdate: (OneFamilyMember, OneRole) -> Unit,
    onRemove: (OneFamilyMember) -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        if (canPromote && !member.role.equals("caregiver", ignoreCase = true)) {
            TextButton(onClick = { onUpdate(member, OneRole.CAREGIVER) }, enabled = !isBusy) { Text("Caregiver") }
        } else if (!member.role.equals("resident", ignoreCase = true)) {
            TextButton(onClick = { onUpdate(member, OneRole.RESIDENT) }, enabled = !isBusy) { Text("Resident") }
        }
        TextButton(onClick = { onRemove(member) }, enabled = !isBusy) { Text("Remove") }
    }
}

@Composable
private fun CareRecipientsCard(
    isBackend: Boolean,
    recipients: List<OneCareRecipient>?,
    loadState: OneCareRecipientLoadState,
    loadError: String?,
    actionState: OneCareRecipientActionState,
    actionError: String?,
    canManage: Boolean,
    onRetry: () -> Unit,
    onCreate: (String, String?, String?) -> Unit,
    onUpdate: (OneCareRecipient, String, String?, String?) -> Unit,
    onDelete: (OneCareRecipient) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var relationship by rememberSaveable { mutableStateOf("") }
    var roomLabel by rememberSaveable { mutableStateOf("") }
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = editingId?.let { id -> recipients.orEmpty().firstOrNull { it.id.toString() == id } }
    val busy = actionState == OneCareRecipientActionState.SUBMITTING

    if (!isBackend) {
        InfoCard("Care recipients are available with a backend", "Connect your ONE account to name the people receiving support in this care space.")
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Care recipients", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (canManage) {
                    OutlinedButton(
                        onClick = {
                            editingId = null
                            displayName = ""
                            relationship = ""
                            roomLabel = ""
                            showDialog = true
                        },
                        enabled = !busy
                    ) { Text("Add") }
                }
            }
            when {
                recipients == null && (loadState == OneCareRecipientLoadState.IDLE || loadState == OneCareRecipientLoadState.LOADING) -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading care recipients…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                recipients == null && loadState == OneCareRecipientLoadState.ERROR -> {
                    Text(loadError ?: "ONE could not load care recipients.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
                recipients.isNullOrEmpty() -> Text("No care recipients have been added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> recipients.orEmpty().forEachIndexed { index, recipient ->
                    if (index > 0) HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(recipient.displayName, style = MaterialTheme.typography.titleSmall)
                            val details = listOfNotNull(recipient.relationship, recipient.roomLabel).joinToString(" · ")
                            Text(details.ifBlank { "Care recipient" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canManage) {
                            TextButton(
                                onClick = {
                                    editingId = recipient.id.toString()
                                    displayName = recipient.displayName
                                    relationship = recipient.relationship.orEmpty()
                                    roomLabel = recipient.roomLabel.orEmpty()
                                    showDialog = true
                                },
                                enabled = !busy
                            ) { Text("Edit") }
                            TextButton(onClick = { deleteTargetId = recipient.id.toString() }, enabled = !busy) { Text("Remove") }
                        }
                    }
                }
            }
            actionError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (showDialog) {
        OneAlertDialog(
            onDismissRequest = { if (!busy) showDialog = false },
            title = { Text(if (editing == null) "Add care recipient" else "Edit care recipient") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This profile represents a person receiving support; it does not create a login.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(120) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relationship, onValueChange = { relationship = it.take(120) }, label = { Text("Relationship (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = roomLabel, onValueChange = { roomLabel = it.take(120) }, label = { Text("Room (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        editing?.let { onUpdate(it, displayName, relationship.takeIf(String::isNotBlank), roomLabel.takeIf(String::isNotBlank)) }
                            ?: onCreate(displayName, relationship.takeIf(String::isNotBlank), roomLabel.takeIf(String::isNotBlank))
                        editingId = null
                    },
                    enabled = displayName.trim().isNotBlank() && !busy
                ) { Text(if (busy) "Saving…" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }, enabled = !busy) { Text("Cancel") } }
        )
    }
    val deleteTarget = deleteTargetId?.let { id -> recipients.orEmpty().firstOrNull { it.id.toString() == id } }
    if (deleteTarget != null) {
        OneAlertDialog(
            onDismissRequest = { if (!busy) deleteTargetId = null },
            title = { Text("Remove ${deleteTarget.displayName}?") },
            text = { Text("This removes the care profile from this space. It does not delete any household member account.") },
            confirmButton = {
                Button(onClick = { deleteTargetId = null; onDelete(deleteTarget) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteTargetId = null }, enabled = !busy) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CareSpacesCard(
    role: OneRole,
    isBackend: Boolean,
    spaces: List<OneCareSpace>?,
    loadState: OneCareSpaceLoadState,
    loadError: String?,
    actionState: OneCareSpaceActionState,
    actionError: String?,
    canCreate: Boolean,
    onRetry: () -> Unit,
    onCreate: (String, String, String) -> Unit,
    onActivate: (OneCareSpace) -> Unit
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var careSetting by rememberSaveable { mutableStateOf("home") }
    var supportFocus by rememberSaveable { mutableStateOf("general") }
    val busy = actionState == OneCareSpaceActionState.SUBMITTING

    if (!isBackend) {
        InfoCard("Care spaces are available with a backend", "Sign in to switch between households or create a separate space for another person.")
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Care spaces", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (canCreate) {
                    OutlinedButton(onClick = { name = ""; careSetting = "home"; supportFocus = "general"; showCreateDialog = true }, enabled = !busy) {
                        Text("New")
                    }
                }
            }
            Text("A care space keeps people, consent and camera data scoped to one household.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                spaces == null && (loadState == OneCareSpaceLoadState.IDLE || loadState == OneCareSpaceLoadState.LOADING) -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading your care spaces…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                spaces == null && loadState == OneCareSpaceLoadState.ERROR -> {
                    Text(loadError ?: "ONE could not load care spaces.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
                spaces.isNullOrEmpty() -> Text("No care spaces are linked to this account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> spaces.orEmpty().forEach { space ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(space.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${space.careSetting.humanLabel()} · ${space.supportFocus.humanLabel()} · ${countLabel(space.recipientCount, "recipient")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Resident: ${space.residentName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (space.active) {
                            Surface(shape = RoundedCornerShape(50), color = OneMint.copy(alpha = 0.14f)) {
                                Text("ACTIVE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = OneMint, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { onActivate(space) }, enabled = !busy) { Text("Use") }
                        }
                    }
                }
            }
            actionError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (role == OneRole.RESIDENT) {
                Text("Ask a caregiver to create or configure a new space for you.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showCreateDialog) {
        OneAlertDialog(
            onDismissRequest = { if (!busy) showCreateDialog = false },
            title = { Text("Create a care space") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(120) }, label = { Text("Space name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Setting", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = careSetting == "home", onClick = { careSetting = "home" }, label = { Text("Home") })
                        FilterChip(selected = careSetting == "residence", onClick = { careSetting = "residence" }, label = { Text("Residence") })
                    }
                    Text("Support focus", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = supportFocus == "general", onClick = { supportFocus = "general" }, label = { Text("General") })
                        FilterChip(selected = supportFocus == "mci", onClick = { supportFocus = "mci" }, label = { Text("MCI") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCreateDialog = false; onCreate(name, careSetting, supportFocus) }, enabled = name.trim().isNotBlank() && !busy) {
                    Text(if (busy) "Creating…" else "Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }, enabled = !busy) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MedicationRow(
    dose: MedicationDose,
    actionKey: String? = null,
    onStatusChange: ((DoseStatus) -> Unit)? = null
) {
    val canConfirm = onStatusChange != null && dose.planId != null && dose.scheduledFor != null && dose.status !in setOf(DoseStatus.TAKEN, DoseStatus.SKIPPED)
    val isUpdating = canConfirm && actionKey == dose.medicationActionKey()
    Card(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${dose.name}. ${dose.time}. ${dose.instructions}. Status: ${dose.status.label}." }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = doseTint(dose.status), modifier = Modifier.size(29.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(dose.name, style = MaterialTheme.typography.titleMedium)
                    Text("${dose.time} · ${dose.instructions}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dose.assignedTo?.let { "Assigned to $it" } ?: "No caregiver assigned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(dose.status.label, style = MaterialTheme.typography.labelSmall, color = doseTint(dose.status), fontWeight = FontWeight.SemiBold)
            }
            if (canConfirm) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStatusChange?.invoke(DoseStatus.TAKEN) },
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OneMint)
                    ) {
                        Text(if (isUpdating) "Saving…" else "Taken")
                    }
                    OutlinedButton(
                        onClick = { onStatusChange?.invoke(DoseStatus.SKIPPED) },
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

private fun MedicationDose.medicationActionKey(): String = "${planId}:${scheduledFor}"

private fun MedicationDose.isOpenReminder(): Boolean = status !in setOf(
    DoseStatus.ACKNOWLEDGED,
    DoseStatus.TAKEN,
    DoseStatus.SKIPPED
)

private fun doseTint(status: DoseStatus): Color = when (status) {
    DoseStatus.ACKNOWLEDGED, DoseStatus.TAKEN -> OneMint
    DoseStatus.NEEDS_CONFIRMATION, DoseStatus.SKIPPED, DoseStatus.MISSED -> OneBlue
    DoseStatus.SCHEDULED, DoseStatus.PENDING -> OneAmber
}

@Composable
private fun EventsScreen(
    events: List<OneEvent>,
    isBackend: Boolean,
    homeLoadState: OneHomeLoadState,
    homeLoadError: String?,
    homeIsStale: Boolean,
    eventStreamState: OneEventStreamState,
    eventStreamError: String?,
    clips: List<OneClip>?,
    clipLoadState: OneClipLoadState,
    clipLoadError: String?,
    onRetry: () -> Unit,
    onClipRetry: () -> Unit,
    onOpenEvent: (OneEvent) -> Unit,
    onClose: (() -> Unit)? = null
) {
    var eventRangeFilter by rememberSaveable { mutableStateOf("7d") }
    var eventKindFilter by rememberSaveable { mutableStateOf("all") }
    var eventClipsOnly by rememberSaveable { mutableStateOf(false) }
    val rangeFilters = listOf("24h" to "24 h", "7d" to "7 days", "30d" to "30 days", "all" to "All")
    val kindFilters = listOf("all" to "All", "check_in" to "Check-ins", "object_observed" to "Objects", "movement" to "Movement", "assistant" to "Assistant")
    val clipEventIds = clips.orEmpty().map { it.eventId }.toSet()
    val cutoff = when (eventRangeFilter) {
        "24h" -> Instant.now().minusSeconds(24 * 60 * 60L)
        "7d" -> Instant.now().minusSeconds(7 * 24 * 60 * 60L)
        "30d" -> Instant.now().minusSeconds(30 * 24 * 60 * 60L)
        else -> null
    }
    val filteredEvents = events.filter { event ->
        val kindMatches = eventKindFilter == "all" || event.kind.name.equals(eventKindFilter, ignoreCase = true)
        val clipMatches = !eventClipsOnly || event.id?.let(clipEventIds::contains) == true
        val rangeMatches = cutoff == null || event.observedAt == null || !event.observedAt.isBefore(cutoff)
        kindMatches && clipMatches && rangeMatches
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onClose != null) {
            item {
                TextButton(onClick = onClose, contentPadding = PaddingValues(0.dp)) {
                    Text("Back", color = OneBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { ScreenHeader("EVENTS", "Reviewable moments", "Recent activity for caregiver attention.") }
        if (!isBackend) {
            item { AssistChip(onClick = { }, enabled = false, label = { Text("Demo preview · illustrative events") }) }
        }
        if (homeIsStale) {
            item { AssistChip(onClick = onRetry, label = { Text("Offline · showing last known events") }) }
        }
        if (isBackend) {
            item {
                val tint = eventStreamState.eventStreamTint()
                Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.12f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(tint, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("Live updates · ${eventStreamState.eventStreamLabel()}", style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (isBackend && eventStreamState == OneEventStreamState.ERROR && !eventStreamError.isNullOrBlank()) {
            item { Text("${eventStreamError} Retrying automatically…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Text("PERIOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rangeFilters) { (value, label) ->
                    FilterChip(
                        selected = eventRangeFilter == value,
                        onClick = { eventRangeFilter = value },
                        label = { Text(label) }
                    )
                }
            }
        }
        item {
            Text("TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(kindFilters) { (value, label) ->
                    FilterChip(
                        selected = eventKindFilter == value,
                        onClick = { eventKindFilter = value },
                        label = { Text(label) }
                    )
                }
                item {
                    FilterChip(
                        selected = eventClipsOnly,
                        onClick = { eventClipsOnly = !eventClipsOnly },
                        label = { Text("With clips") }
                    )
                }
            }
        }
        item {
            Text(
                if (eventClipsOnly) "Showing ${countLabel(filteredEvents.size, "event")} with linked clips." else "Showing ${countLabel(filteredEvents.size, "event")} in the selected period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isBackend) {
            when {
                clipLoadState == OneClipLoadState.LOADING && clips == null -> item {
                    Text("Checking linked event clips…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                clipLoadState == OneClipLoadState.ERROR -> item {
                    Text(clipLoadError ?: "ONE could not load linked event clips.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onClipRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry clip check") }
                }
            }
        }
        when {
            isBackend && homeLoadState == OneHomeLoadState.LOADING && events.isEmpty() -> item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            isBackend && homeLoadState == OneHomeLoadState.ERROR && events.isEmpty() -> item {
                InfoCard("Events unavailable", homeLoadError ?: "ONE could not load the household events.")
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            events.isEmpty() -> item {
                InfoCard("No events recorded yet", "Reviewable moments will appear here when ONE observes activity.")
            }
            filteredEvents.isEmpty() -> item {
                InfoCard("No matching events", "Try a wider period or another event type.")
            }
            else -> items(filteredEvents) { event ->
                EventRow(
                    event = event,
                    hasClip = event.id?.let(clipEventIds::contains) == true,
                    onClick = { onOpenEvent(event) }
                )
            }
        }
        item { Text("Observations support human attention. They are not a diagnosis.", style = MaterialTheme.typography.bodySmall, color = OneAmber, modifier = Modifier.padding(top = 4.dp)) }
    }
}

private fun OneEventStreamState.eventStreamLabel(): String = when (this) {
    OneEventStreamState.IDLE -> "Paused"
    OneEventStreamState.CONNECTING -> "Connecting…"
    OneEventStreamState.CONNECTED -> "Listening"
    OneEventStreamState.ERROR -> "Retrying"
}

private fun OneEventStreamState.eventStreamTint(): Color = when (this) {
    OneEventStreamState.IDLE -> OneBlue
    OneEventStreamState.CONNECTING -> OneAmber
    OneEventStreamState.CONNECTED -> OneMint
    OneEventStreamState.ERROR -> OneAmber
}

@Composable
private fun EventRow(event: OneEvent, hasClip: Boolean = false, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .semantics(mergeDescendants = true) {
                contentDescription = "${event.kind.label}. ${event.explanation}. ${event.location}, ${event.time}. ${event.confidence}${if (event.evidenceIds.isNotEmpty()) ". ${event.evidenceIds.size} linked source records." else ""}${if (hasClip) " Consent-based clip available." else ""}"
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).background(OneBlue.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = OneBlue, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.kind.label, style = MaterialTheme.typography.titleMedium)
                Text(event.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${event.location} · ${event.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(event.confidence, style = MaterialTheme.typography.labelSmall, color = if (event.confidence.startsWith("High")) OneMint else OneAmber, fontWeight = FontWeight.SemiBold)
            if (hasClip) {
                Spacer(Modifier.width(7.dp))
                Surface(shape = RoundedCornerShape(50), color = OneCyan.copy(alpha = 0.14f)) {
                    Text("CLIP", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
                }
            }
            if (onClick != null) {
                Spacer(Modifier.width(7.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Review event", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EventDetailScreen(
    event: OneEvent,
    apiClient: OneApiClient,
    session: OneSession?,
    clips: List<OneClip>?,
    clipLoadState: OneClipLoadState,
    clipLoadError: String?,
    onClose: () -> Unit
) {
    val linkedClips = event.id?.let { eventId -> clips?.filter { it.eventId == eventId } }.orEmpty()
    ScreenScroll {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).offset(x = (-3).dp).rotate(180f)
                )
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }
        }
        ScreenHeader("EVENT REVIEW", event.kind.label, "Review the context and evidence.")
        EventRow(event, hasClip = !linkedClips.isNullOrEmpty())
        SectionHeading("CONTEXT", "What ONE observed")
        InfoCard("Approximate location", event.location)
        InfoCard("When it happened", event.time)
        InfoCard("Explanation", event.explanation)
        InfoCard("Confidence", event.confidence)
        InfoCard(
            "Source evidence",
            if (event.evidenceIds.isEmpty()) "No source record was linked to this event." else "Linked record IDs: ${event.evidenceIds.take(5).joinToString(", ")}${if (event.evidenceIds.size > 5) "…" else ""}"
        )
        if (event.id != null) {
            when {
                clipLoadState == OneClipLoadState.LOADING && clips == null -> InfoCard("Linked evidence", "Checking whether this observation has a consented event clip.")
                clipLoadState == OneClipLoadState.ERROR -> InfoCard("Linked evidence unavailable", clipLoadError ?: "ONE could not check event clips right now.")
                linkedClips.isNotEmpty() -> InfoCard("Linked evidence", "A consented event clip is linked to this observation.")
                else -> InfoCard("Linked evidence", "No retained event clip is linked to this observation.")
            }
        }
        if (linkedClips.isNotEmpty() && session != null) {
            SectionHeading("EVIDENCE", "Consent-based event clip")
            ClipPlayer(apiClient = apiClient, session = session, clip = linkedClips.first())
            if (linkedClips.size > 1) {
                Text("${linkedClips.size} clips are linked to this observation; showing the most recent one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        InfoCard("Human review", "This observation can support attention and discussion. It is not a diagnosis or medical advice.")
    }
}

@Composable
@OptIn(markerClass = [UnstableApi::class])
private fun ClipPlayer(apiClient: OneApiClient, session: OneSession, clip: OneClip) {
    val context = LocalContext.current
    var playbackState by remember(clip.id, session.accessToken) { mutableIntStateOf(Player.STATE_IDLE) }
    var isPlaying by remember(clip.id, session.accessToken) { mutableStateOf(false) }
    var playbackError by remember(clip.id, session.accessToken) { mutableStateOf<String?>(null) }
    val player = remember(clip.id, session.accessToken, apiClient) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                mapOf(
                    "Authorization" to "Bearer ${session.accessToken}",
                    "Accept" to "video/mp4"
                )
            )
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)
        ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "The event clip could not be played."
            }
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(apiClient.clipContentUrl(session, clip.id)))
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OneInverseSurface),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = true
                        this.player = player
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize()
            )
            playbackError?.let { error ->
                Surface(
                    modifier = Modifier.padding(18.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.72f)
                ) {
                    Text(error, modifier = Modifier.padding(14.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text(
            text = when {
                playbackError != null -> "Playback unavailable"
                isPlaying -> "Playing consented event clip"
                playbackState == Player.STATE_BUFFERING -> "Preparing clip…"
                playbackState == Player.STATE_ENDED -> "Clip finished"
                playbackState == Player.STATE_READY -> "Press play to review the clip"
                else -> "Loading clip…"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (playbackError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResidentTodayScreen(
    isBackend: Boolean,
    medicationDoses: List<MedicationDose>?,
    medicationLoadState: OneMedicationLoadState,
    medicationLoadError: String?,
    onMedicationRetry: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    val todayDoses = (if (isBackend) medicationDoses.orEmpty() else demoMedicationDoses)
        .sortedWith(compareBy<MedicationDose> { it.scheduledFor ?: Instant.MAX }.thenBy { it.time })
    val nextDose = todayDoses.firstOrNull { it.status !in setOf(DoseStatus.TAKEN, DoseStatus.SKIPPED) }

    ScreenScroll {
        ScreenHeader("TODAY", "A more independent day.", "Support when you need it.")
        if (nextDose != null) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = OneBlue.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(OneBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("NEXT REMINDER", style = MaterialTheme.typography.labelSmall, color = OneBlue, fontWeight = FontWeight.Bold)
                        Text(nextDose.time, style = MaterialTheme.typography.headlineSmall, color = OneBlue, fontWeight = FontWeight.Bold)
                        Text(nextDose.name, style = MaterialTheme.typography.titleMedium)
                        Text(nextDose.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SectionHeading("MEDICATIONS", "Today's reminders")
        if (isBackend) {
            when {
                medicationDoses == null && medicationLoadState in setOf(OneMedicationLoadState.IDLE, OneMedicationLoadState.LOADING) -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading today's reminders…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                medicationDoses == null && medicationLoadState == OneMedicationLoadState.ERROR -> {
                    InfoCard("Medication data unavailable", medicationLoadError ?: "ONE could not load today's reminders.")
                    OutlinedButton(onClick = onMedicationRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
                todayDoses.isEmpty() -> InfoCard("No reminders for today", "Your care team has not scheduled a medication reminder for today.")
                else -> todayDoses.forEach { dose -> ResidentMedicationRow(dose) }
            }
        } else {
            todayDoses.forEach { dose -> ResidentMedicationRow(dose) }
            Text("Demo reminders are examples. Follow the plan agreed with your care team.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OneMint, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your check-in is ready", style = MaterialTheme.typography.titleMedium)
                    Text("Tell ONE how the day is going when you feel ready.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Button(onClick = onOpenAssistant, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = OneBlue)) {
            Icon(Icons.Default.GraphicEq, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text("Open assistant", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResidentMedicationRow(dose: MedicationDose) {
    val tint = doseTint(dose.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${dose.name}. ${dose.time}. ${dose.instructions}. Status: ${dose.status.label}."
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dose.time, style = MaterialTheme.typography.titleMedium, color = OneBlue, fontWeight = FontWeight.Bold)
                Text("TIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(dose.name, style = MaterialTheme.typography.titleMedium)
                Text(dose.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                dose.assignedTo?.let { caregiver ->
                    Text("Support: $caregiver", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = 0.12f)) {
                Text(
                    text = dose.status.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AssistantScreen(
    isBackend: Boolean,
    audioConsentGranted: Boolean,
    loadState: OneAssistantLoadState,
    result: OneCheckInResult?,
    loadError: String?,
    onSubmit: (String) -> Unit
) {
    val assistantContext = LocalContext.current
    var transcript by rememberSaveable { mutableStateOf("") }
    var isListening by rememberSaveable { mutableStateOf(false) }
    var speechError by rememberSaveable { mutableStateOf<String?>(null) }
    var microphoneGranted by remember(assistantContext) {
        mutableStateOf(ContextCompat.checkSelfPermission(assistantContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        microphoneGranted = granted
        speechError = if (granted) null else "Microphone permission is required for push-to-talk."
    }
    val speechRecognizer = remember(assistantContext) {
        OneSpeechRecognizer(
            context = assistantContext,
            onText = { text -> transcript = text.take(4_000) },
            onListeningChanged = { isListening = it },
            onError = { speechError = it }
        )
    }
    DisposableEffect(speechRecognizer) { onDispose { speechRecognizer.dispose() } }
    val pushToTalkAllowed = !isBackend || audioConsentGranted

    ScreenScroll {
        ScreenHeader("ASSISTANT", "I'm here with you.", "A calm daily check-in.")
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = OneBlue, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("A conversation for today", style = MaterialTheme.typography.titleMedium)
                    Text("Tell ONE how you feel, type a note, or press and hold to talk. Your reminders stay on Today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (isBackend && !audioConsentGranted) {
            InfoCard("Push-to-talk is paused", "Enable microphone consent in Account before recording audio. You can still type a check-in here.")
        }
        result?.let { checkIn ->
            SectionHeading("LATEST CHECK-IN", "A human-readable summary")
            InfoCard("${checkIn.status.humanLabel()} · ${checkIn.trend.humanLabel()}", checkIn.explanation)
            if (checkIn.evidenceIds.isNotEmpty()) {
                Text("Based on ${countLabel(checkIn.evidenceIds.size, "recent household observation")}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Limitations: ${checkIn.limitations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (checkIn.degraded) {
                Text("This response used a limited fallback summary.", style = MaterialTheme.typography.bodySmall, color = OneAmber)
            }
        }
        loadError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            value = transcript,
            onValueChange = { transcript = it.take(4_000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Today's check-in") },
            placeholder = { Text("For example: I feel ready for breakfast…") },
            minLines = 3,
            maxLines = 5,
            supportingText = { Text("${transcript.length}/4000") }
        )
        if (!pushToTalkAllowed) {
            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enable microphone consent in Account")
            }
        } else if (!microphoneGranted) {
            OutlinedButton(
                onClick = { microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enable microphone for push-to-talk")
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isListening) OneMint else OneBlue)
                    .pointerInput(microphoneGranted) {
                        detectTapGestures(
                            onPress = {
                                speechError = null
                                speechRecognizer.start()
                                tryAwaitRelease()
                                speechRecognizer.stop()
                            }
                        )
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isListening) "Listening. Release to finish speaking." else "Press and hold to talk."
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(9.dp))
                    Text(if (isListening) "Listening… release to finish" else "Press and hold to talk", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        speechError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { onSubmit(transcript) },
            enabled = transcript.isNotBlank() && loadState != OneAssistantLoadState.SUBMITTING,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OneBlue)
        ) {
            Icon(Icons.Default.GraphicEq, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text(if (loadState == OneAssistantLoadState.SUBMITTING) "Sending…" else "Send check-in", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            if (isBackend) "The backend uses recent bounded observations and returns limitations with every summary."
            else "Demo mode keeps this check-in on the device; connect a backend for a household summary.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("ONE supports attention and conversation. It does not diagnose or make medical decisions.", style = MaterialTheme.typography.bodySmall, color = OneAmber)
    }
}

private fun String.humanLabel(): String = replace('_', ' ').replace('-', ' ').replaceFirstChar { it.uppercase() }

@Composable
@kotlin.OptIn(ExperimentalMaterial3Api::class)
private fun AccountScreen(
    role: OneRole,
    isBackend: Boolean,
    backendHealth: BackendHealth?,
    backendHealthLoadState: OneBackendHealthLoadState,
    backendHealthError: String?,
    onBackendHealthRetry: () -> Unit,
    consentStates: Map<String, Boolean>?,
    consentSubjectName: String,
    consentLoadState: OneConsentLoadState,
    consentLoadError: String?,
    consentUpdatePurpose: String?,
    consentUpdateError: String?,
    onConsentRetry: () -> Unit,
    onConsentChange: (String, Boolean) -> Unit,
    exportLoadState: OneExportLoadState,
    dataExport: OneDataExport?,
    exportLoadError: String?,
    onExport: () -> Unit,
    isAdmin: Boolean,
    deletionLoadState: OneDeletionLoadState,
    dataDeletion: OneDataDeletion?,
    deletionLoadError: String?,
    onDelete: () -> Unit,
    careSpaces: List<OneCareSpace>?,
    careSpacesLoadState: OneCareSpaceLoadState,
    careSpacesLoadError: String?,
    careSpaceActionState: OneCareSpaceActionState,
    careSpaceActionError: String?,
    canCreateCareSpace: Boolean,
    onCareSpacesRetry: () -> Unit,
    onCreateCareSpace: (String, String, String) -> Unit,
    onActivateCareSpace: (OneCareSpace) -> Unit,
    onRoleChange: (OneRole) -> Unit,
    onSignOut: () -> Unit
) {
    val accountContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationPermissionRequestGranted by remember { mutableStateOf<Boolean?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionRequestGranted = granted
    }
    DisposableEffect(lifecycleOwner, accountContext) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionRequestGranted =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(accountContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val systemNotificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(accountContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notificationsEnabled = systemNotificationsEnabled || notificationPermissionRequestGranted == true
    var demoRoomConsent by rememberSaveable { mutableStateOf(true) }
    var demoMicrophoneConsent by rememberSaveable { mutableStateOf(true) }
    var demoMedicationConsent by rememberSaveable { mutableStateOf(true) }
    var demoFamilyConsent by rememberSaveable { mutableStateOf(false) }
    var demoFamilyAssistantConsent by rememberSaveable { mutableStateOf(false) }
    var showCareSpaces by rememberSaveable { mutableStateOf(false) }
    var showDeletionConfirmation by rememberSaveable { mutableStateOf(false) }
    val canEditConsents = !isBackend || consentLoadState == OneConsentLoadState.LOADED
    val canRequestDeletion = isBackend && isAdmin && deletionLoadState != OneDeletionLoadState.SUBMITTING && dataDeletion == null
    val consentValue: (String, Boolean) -> Boolean = { purpose, demoValue ->
        if (isBackend) consentStates?.get(purpose) ?: false else demoValue
    }
    val updateEnabled: (String) -> Boolean = { purpose ->
        canEditConsents && consentUpdatePurpose == null && (isBackend || purpose.isNotBlank())
    }

    ScreenScroll {
        ScreenHeader("ACCOUNT", "Privacy and control.", "Your home, your choices.")
        CareSpaceContextCard(
            space = careSpaces?.firstOrNull { it.active } ?: careSpaces?.firstOrNull(),
            isLoading = careSpacesLoadState == OneCareSpaceLoadState.LOADING,
            onClick = { showCareSpaces = true }
        )
        SectionHeading("PRIVACY", "Privacy and consent")
        if (isBackend) {
            when {
                consentLoadState == OneConsentLoadState.LOADING && consentStates == null -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading privacy settings…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                consentLoadState == OneConsentLoadState.ERROR && consentStates == null -> {
                    InfoCard("Privacy settings unavailable", consentLoadError ?: "ONE could not load the household consents.")
                    OutlinedButton(onClick = onConsentRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                }
            }
        }
        SectionHeading("NOTIFICATIONS", "Medication reminders")
        InfoCard(
            if (notificationsEnabled) "Notifications enabled" else "Notifications are off",
            if (notificationsEnabled) "ONE will alert the resident at scheduled medication times and repeat daily rules." else "Allow notifications so a scheduled dose is not easy to miss."
        )
        if (!notificationsEnabled) {
            OutlinedButton(
                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Allow medication notifications") }
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                ConsentRow(
                    label = "Room and camera data",
                    enabled = consentValue("video_capture", demoRoomConsent),
                    interactive = updateEnabled("video_capture"),
                    onChanged = { granted -> if (isBackend) onConsentChange("video_capture", granted) else demoRoomConsent = granted }
                )
                HorizontalDivider()
                ConsentRow(
                    label = "Microphone for push-to-talk",
                    enabled = consentValue("audio_capture", demoMicrophoneConsent),
                    interactive = updateEnabled("audio_capture"),
                    onChanged = { granted -> if (isBackend) onConsentChange("audio_capture", granted) else demoMicrophoneConsent = granted }
                )
                HorizontalDivider()
                ConsentRow(
                    label = "Medication reminders",
                    enabled = consentValue("medication_management", demoMedicationConsent),
                    interactive = updateEnabled("medication_management"),
                    onChanged = { granted -> if (isBackend) onConsentChange("medication_management", granted) else demoMedicationConsent = granted }
                )
                HorizontalDivider()
                ConsentRow(
                    label = "Family sharing",
                    enabled = consentValue("family_mode", demoFamilyConsent),
                    interactive = updateEnabled("family_mode"),
                    onChanged = { granted -> if (isBackend) onConsentChange("family_mode", granted) else demoFamilyConsent = granted }
                )
                HorizontalDivider()
                ConsentRow(
                    label = "Family assistant summary",
                    enabled = consentValue("family_assistant", demoFamilyAssistantConsent),
                    interactive = updateEnabled("family_assistant"),
                    onChanged = { granted -> if (isBackend) onConsentChange("family_assistant", granted) else demoFamilyAssistantConsent = granted }
                )
            }
        }
        Text("Privacy choices below apply to $consentSubjectName. A caregiver must not infer consent for another person; switch to that person's signed-in account or record their choice through the care workflow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        consentUpdateError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text("Room and camera consent also governs short event clips. Sensitive data stays local unless you explicitly enable sharing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BackendStatusSection(
            isBackend = isBackend,
            backendHealth = backendHealth,
            backendHealthLoadState = backendHealthLoadState,
            backendHealthError = backendHealthError,
            onBackendHealthRetry = onBackendHealthRetry
        )
        SectionHeading("YOUR DATA", "Human control")
        OutlinedButton(
            onClick = onExport,
            enabled = exportLoadState != OneExportLoadState.SUBMITTING,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (exportLoadState == OneExportLoadState.SUBMITTING) "Preparing…" else "Prepare a data export")
        }
        exportLoadError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        dataExport?.let { export ->
            val totalRecords = export.recordCounts.values.sum()
            val recordSummary = export.recordCounts.entries
                .sortedByDescending { it.value }
                .take(4)
                .joinToString(" · ") { "${it.key.humanLabel()}: ${it.value}" }
            InfoCard(
                "Export prepared",
                "${export.exportedAt?.toHumanDateTime() ?: "Timestamp unavailable"} · ${countLabel(totalRecords, "record")}${recordSummary.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}. The payload is not stored on this device."
            )
        }
        OutlinedButton(
            onClick = { showDeletionConfirmation = true },
            enabled = canRequestDeletion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    deletionLoadState == OneDeletionLoadState.SUBMITTING -> "Deleting…"
                    dataDeletion != null -> "Deletion completed"
                    else -> "Request deletion"
                }
            )
        }
        when {
            !isBackend -> Text(
                "Connect a backend session to manage household deletion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            !isAdmin -> Text(
                "Only the household administrator can request deletion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        deletionLoadError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        dataDeletion?.let { deletion ->
            val status = deletion.status.replace('_', ' ').replaceFirstChar { it.uppercase() }
            InfoCard(
                "Deletion completed",
                "Status: $status · Request ${deletion.requestId ?: "recorded"}. Household data is no longer available."
            )
        }
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(if (dataDeletion != null) "Finish and sign out" else "Preview signed-out flow")
        }
        SectionHeading("DEMO", "Preview another experience")
        OutlinedButton(onClick = { onRoleChange(if (role == OneRole.CAREGIVER) OneRole.RESIDENT else OneRole.CAREGIVER) }, modifier = Modifier.fillMaxWidth()) {
            Icon(if (role == OneRole.CAREGIVER) Icons.Default.Person else Icons.Default.People, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (role == OneRole.CAREGIVER) "Preview resident experience" else "Preview caregiver experience")
        }
        Text("Observations support human attention. They are not medical advice or a diagnosis.", style = MaterialTheme.typography.bodySmall, color = OneAmber)
    }
    if (showCareSpaces) {
        ModalBottomSheet(onDismissRequest = { showCareSpaces = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScreenHeader("CARE SPACES", "Choose where you’re caring.", "Each care space keeps its people, cameras, maps, routines and consent separate.")
                CareSpacesCard(
                    role = role,
                    isBackend = isBackend,
                    spaces = careSpaces,
                    loadState = careSpacesLoadState,
                    loadError = careSpacesLoadError,
                    actionState = careSpaceActionState,
                    actionError = careSpaceActionError,
                    canCreate = canCreateCareSpace,
                    onRetry = onCareSpacesRetry,
                    onCreate = onCreateCareSpace,
                    onActivate = onActivateCareSpace
                )
            }
        }
    }
    if (showDeletionConfirmation) {
        OneAlertDialog(
            onDismissRequest = { showDeletionConfirmation = false },
            title = { Text("Delete household data?") },
            text = {
                Text("This permanently removes the household records, maps, clips, events and medication history. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletionConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletionConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BackendStatusSection(
    isBackend: Boolean,
    backendHealth: BackendHealth?,
    backendHealthLoadState: OneBackendHealthLoadState,
    backendHealthError: String?,
    onBackendHealthRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("BACKEND", "Connection status")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Connection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    !isBackend -> {
                        Text("Demo mode · no backend session is connected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    backendHealthLoadState == OneBackendHealthLoadState.LOADING && backendHealth == null -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Checking API health…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    backendHealthLoadState == OneBackendHealthLoadState.ERROR && backendHealth == null -> {
                        Text(backendHealthError ?: "ONE could not reach the backend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onBackendHealthRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry backend check") }
                    }
                    backendHealth != null -> {
                        val health = backendHealth
                        val statusTint = if (health.status.equals("ok", ignoreCase = true)) OneMint else OneAmber
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).background(statusTint, CircleShape))
                            Spacer(Modifier.width(7.dp))
                            Text("API ${health.status.humanLabel()}", style = MaterialTheme.typography.titleSmall, color = statusTint, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Database: ${health.databaseStatus ?: health.database ?: "unknown"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        health.localInferenceModel?.let { model ->
                            Text("Inference model: $model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (backendHealthLoadState == OneBackendHealthLoadState.ERROR) {
                            Text(backendHealthError ?: "The last health check failed; showing the previous result.", style = MaterialTheme.typography.bodySmall, color = OneAmber)
                            OutlinedButton(onClick = onBackendHealthRetry, modifier = Modifier.fillMaxWidth()) { Text("Check again") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentRow(label: String, enabled: Boolean, interactive: Boolean = true, onChanged: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = enabled, enabled = interactive, onCheckedChange = onChanged)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OneAppPreview() {
    ONETheme { OneApp() }
}
