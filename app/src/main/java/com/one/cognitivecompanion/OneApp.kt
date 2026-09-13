package com.one.cognitivecompanion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.one.cognitivecompanion.ui.theme.OneAmber
import com.one.cognitivecompanion.ui.theme.OneBlue
import com.one.cognitivecompanion.ui.theme.OneCyan
import com.one.cognitivecompanion.ui.theme.OneInverseSurface
import com.one.cognitivecompanion.ui.theme.OneMint
import com.one.cognitivecompanion.ui.theme.ONETheme
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.track.Track
import kotlinx.coroutines.launch

private data class OneNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

private val caregiverTabs = listOf(
    OneNavItem("home", "Home", Icons.Default.Home),
    OneNavItem("map", "Map", Icons.Default.Map),
    OneNavItem("family", "Family", Icons.Default.People),
    OneNavItem("events", "Events", Icons.Default.Notifications),
    OneNavItem("account", "Account", Icons.Default.AccountCircle)
)

private val residentTabs = listOf(
    OneNavItem("today", "Today", Icons.Default.WbSunny),
    OneNavItem("assistant", "Assistant", Icons.Default.GraphicEq),
    OneNavItem("account", "Account", Icons.Default.AccountCircle)
)

@Composable
fun OneApp() {
    // Demo mode starts inside the app so the shell is immediately usable. The
    // Account screen exposes a signed-out preview for demo and backend auth.
    val appContext = LocalContext.current.applicationContext
    val secureStore = remember(appContext) { OneSecureStore(appContext) }
    val apiClient = remember { OneHttpApiClient() }
    val homeRepository = remember(apiClient) { OneApiHomeRepository(apiClient) }
    val cameraRepository = remember(apiClient) { OneApiCameraRepository(apiClient) }
    val familyRepository = remember(apiClient) { OneApiFamilyRepository(apiClient) }
    val medicationRepository = remember(apiClient) { OneApiMedicationRepository(apiClient) }
    val clipRepository = remember(apiClient) { OneApiClipRepository(apiClient) }
    val appState = remember(secureStore, apiClient, homeRepository, cameraRepository, familyRepository, medicationRepository, clipRepository) {
        OneAppState(apiClient, secureStore, homeRepository, cameraRepository, familyRepository, medicationRepository, clipRepository)
    }
    var authStageName by appState::authStageName
    var roleName by appState::roleName
    var selectedTab by appState::selectedTab
    var onboardingStep by appState::onboardingStep
    var onboardingConsentRoom by appState::onboardingConsentRoom
    var onboardingConsentMic by appState::onboardingConsentMic
    var onboardingConsentMedication by appState::onboardingConsentMedication
    var onboardingConsentFamily by appState::onboardingConsentFamily
    var selectedCameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEvent by remember { mutableStateOf<OneEvent?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val authStage = AuthStage.valueOf(authStageName)
    val role = OneRole.valueOf(roleName)
    val tabs = if (role == OneRole.CAREGIVER) caregiverTabs else residentTabs
    val selectedCamera = appState.cameras?.firstOrNull { it.id.toString() == selectedCameraId }

    LaunchedEffect(appState) { appState.restoreSession() }
    LaunchedEffect(appState, appState.authStageName, appState.session) {
        if (appState.authStageName == AuthStage.AUTHENTICATED.name) {
            appState.loadHome()
            appState.loadCameras()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.roleName, appState.selectedTab) {
        if (
            appState.authStageName == AuthStage.AUTHENTICATED.name &&
            appState.roleName == OneRole.CAREGIVER.name &&
            appState.selectedTab == "events"
        ) {
            appState.loadClips()
            appState.observeHomeEvents()
        }
    }
    LaunchedEffect(appState, appState.authStageName, appState.session, appState.selectedTab) {
        if (appState.authStageName == AuthStage.AUTHENTICATED.name && appState.selectedTab == "family") {
            appState.loadFamily()
            appState.loadMedicationReminders()
        }
    }

    if (authStage == AuthStage.AUTHENTICATED && tabs.none { it.key == selectedTab }) selectedTab = tabs.first().key

    Scaffold(
        bottomBar = if (authStage == AuthStage.AUTHENTICATED && selectedCamera == null && selectedEvent == null) {
            {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.key,
                        onClick = { selectedTab = tab.key },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
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
                    onRoomConsentChange = { onboardingConsentRoom = it },
                    onMicrophoneConsentChange = { onboardingConsentMic = it },
                    onMedicationConsentChange = { onboardingConsentMedication = it },
                    onFamilyConsentChange = { onboardingConsentFamily = it },
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
                } else when (role) {
                    OneRole.CAREGIVER -> when (selectedTab) {
                        "map" -> MapScreen(
                            objects = appState.homeSnapshot?.objects,
                            events = appState.homeSnapshot?.events,
                            isBackend = appState.backendMode,
                            loadState = appState.homeLoadState,
                            loadError = appState.homeLoadError,
                            onRetry = { coroutineScope.launch { appState.loadHome() } }
                        )
                        "family" -> FamilyScreen(
                            members = appState.familyMembers,
                            isBackend = appState.backendMode,
                            loadState = appState.familyLoadState,
                            loadError = appState.familyLoadError,
                            onRetry = { coroutineScope.launch { appState.loadFamily() } },
                            medicationDoses = appState.medicationDoses,
                            medicationLoadState = appState.medicationLoadState,
                            medicationLoadError = appState.medicationLoadError,
                            onMedicationRetry = { coroutineScope.launch { appState.loadMedicationReminders() } },
                            medicationActionKey = appState.medicationActionKey,
                            medicationActionError = appState.medicationActionError,
                            onMedicationStatusChange = { dose, status ->
                                coroutineScope.launch { appState.updateMedicationDose(dose, status) }
                            }
                        )
                        "events" -> EventsScreen(
                            events = appState.homeSnapshot?.events ?: if (appState.backendMode) emptyList() else demoEvents,
                            isBackend = appState.backendMode,
                            homeLoadState = appState.homeLoadState,
                            homeLoadError = appState.homeLoadError,
                            eventStreamState = appState.eventStreamState,
                            eventStreamError = appState.eventStreamError,
                            clips = appState.clips,
                            clipLoadState = appState.clipLoadState,
                            clipLoadError = appState.clipLoadError,
                            onRetry = { coroutineScope.launch { appState.loadHome() } },
                            onClipRetry = { coroutineScope.launch { appState.loadClips() } },
                            onOpenEvent = { selectedEvent = it }
                        )
                        "account" -> AccountScreen(
                            role = role,
                            onRoleChange = { roleName = it.name; selectedTab = if (it == OneRole.RESIDENT) "today" else "home" },
                            onSignOut = { coroutineScope.launch { appState.signOut() } }
                        )
                        else -> CaregiverHomeScreen(
                            homeSnapshot = appState.homeSnapshot,
                            homeLoadState = appState.homeLoadState,
                            homeLoadError = appState.homeLoadError,
                            onRetry = { coroutineScope.launch { appState.loadHome() } },
                            cameras = appState.cameras,
                            cameraLoadState = appState.cameraLoadState,
                            cameraLoadError = appState.cameraLoadError,
                            onCameraRetry = { coroutineScope.launch { appState.loadCameras() } },
                            onOpenCamera = { selectedCameraId = it.id.toString() }
                        )
                    }
                    OneRole.RESIDENT -> when (selectedTab) {
                        "assistant" -> AssistantScreen(
                            isBackend = appState.backendMode,
                            loadState = appState.assistantLoadState,
                            result = appState.assistantResult,
                            loadError = appState.assistantLoadError,
                            onSubmit = { transcript ->
                                coroutineScope.launch { appState.submitAssistantCheckIn(transcript) }
                            }
                        )
                        "account" -> AccountScreen(
                            role = role,
                            onRoleChange = { roleName = it.name; selectedTab = if (it == OneRole.RESIDENT) "today" else "home" },
                            onSignOut = { coroutineScope.launch { appState.signOut() } }
                        )
                        else -> ResidentTodayScreen(onOpenAssistant = { selectedTab = "assistant" })
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
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var mode by rememberSaveable { mutableStateOf(0) }
    var pairingCode by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var homeName by rememberSaveable { mutableStateOf("") }
    var accountConsent by rememberSaveable { mutableStateOf(false) }
    var useBackend by rememberSaveable { mutableStateOf(false) }
    var backendStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val isCreateMode = mode == 1
    val codeIsValid = pairingCode.length == 6 && pairingCode.all(Char::isDigit)
    val canContinue = if (isCreateMode) name.isNotBlank() && accountConsent else if (useBackend) codeIsValid else pairingCode.isNotBlank()

    LaunchedEffect(useBackend) {
        errorMessage = null
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
            Tab(selected = mode == 0, onClick = { mode = 0 }, text = { Text("Sign in") })
            Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text("Create household") })
            Tab(selected = mode == 2, onClick = { mode = 2 }, text = { Text("Join household") })
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Use ONE backend", style = MaterialTheme.typography.titleMedium)
                Text("Connect this device to a running FastAPI home.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = useBackend, onCheckedChange = { useBackend = it })
        }
        backendStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (it == "Backend connected") OneMint else MaterialTheme.colorScheme.onSurfaceVariant) }
        if (isCreateMode) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = homeName, onValueChange = { homeName = it }, label = { Text("Household name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            ConsentRow("I consent to ONE storing the account data needed for this service.", accountConsent) { accountConsent = it }
        } else {
            OutlinedTextField(
                value = pairingCode,
                onValueChange = { pairingCode = it.uppercase() },
                label = { Text(if (mode == 0) "Pairing code" else "Invitation code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (mode == 2) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = OneAmber) }
        Text(
            if (useBackend) "Codes are six digits and are used only once." else "Demo mode is active. Any non-empty code continues without a server.",
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
                            val authenticated = when {
                                mode == 1 -> {
                                    val pairing = apiClient.startPairing(
                                        PairingStartRequest(
                                            displayName = name.trim(),
                                            email = email.trim().ifBlank { null },
                                            homeName = homeName.trim().ifBlank { "ONE Home" }
                                        )
                                    )
                                    apiClient.completePairing(pairing.pairingCode)
                                }
                                mode == 2 -> apiClient.acceptFamilyInvite(FamilyInviteAcceptRequest(pairingCode, name.trim().ifBlank { null }))
                                else -> apiClient.completePairing(pairingCode)
                            }
                            onAuthenticated(authenticated, true)
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
            Text(if (isSubmitting) "Working…" else if (mode == 0) "Sign in" else if (mode == 1) "Create account" else "Join household", style = MaterialTheme.typography.titleMedium)
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
    onRoomConsentChange: (Boolean) -> Unit,
    onMicrophoneConsentChange: (Boolean) -> Unit,
    onMedicationConsentChange: (Boolean) -> Unit,
    onFamilyConsentChange: (Boolean) -> Unit,
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
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CaregiverHomeScreen(
    homeSnapshot: OneHomeSnapshot?,
    homeLoadState: OneHomeLoadState,
    homeLoadError: String?,
    onRetry: () -> Unit,
    cameras: List<OneCamera>?,
    cameraLoadState: OneCameraLoadState,
    cameraLoadError: String?,
    onCameraRetry: () -> Unit,
    onOpenCamera: (OneCamera) -> Unit
) {
    val isBackendHome = homeLoadState != OneHomeLoadState.IDLE || homeSnapshot != null
    ScreenScroll {
        ScreenHeader(
            eyebrow = "ONE",
            title = homeSnapshot?.profile?.homeName?.let { "$it, in view." } ?: "Your home, in view.",
            subtitle = homeSnapshot?.profile?.residentName?.let { "A calm view of ${it}'s home, with consent." }
                ?: "A calm, human-readable picture of today."
        )
        when {
            homeLoadState == OneHomeLoadState.LOADING && homeSnapshot == null -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            homeLoadState == OneHomeLoadState.ERROR && homeSnapshot == null -> {
                InfoCard("Home data unavailable", homeLoadError ?: "ONE could not reach the household right now.")
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            isBackendHome -> HomeCameraStatusCard(
                paused = homeSnapshot?.profile?.paused == true,
                cameras = cameras,
                loadState = cameraLoadState,
                loadError = cameraLoadError,
                onRetry = onCameraRetry,
                onOpenCamera = onOpenCamera
            )
            else -> CameraHeroCard()
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("Today", "Objects", "Cameras", "Check-in")) { label ->
                FilterChip(
                    selected = label == "Today",
                    onClick = { },
                    label = { Text(label) }
                )
            }
        }
        SectionHeading("TODAY", "Observed objects")
        if (homeSnapshot != null) {
            if (homeSnapshot.objects.isEmpty()) {
                InfoCard("No objects recorded yet", "Objects appear here after a consented room setup or observation.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(homeSnapshot.objects) { remoteObject ->
                        ObjectCard(
                            title = remoteObject.label,
                            subtitle = remoteObject.zone ?: if (remoteObject.status == "seen") "Home · observed" else "Home · not observed yet",
                            icon = Icons.Default.Visibility,
                            accent = OneCyan
                        )
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item { ObjectCard("Blue mug", "Kitchen · remembered", Icons.Default.Visibility, OneCyan) }
                item { ObjectCard("Front door", "Entry · mapped", Icons.Default.Home, OneBlue) }
                item { ObjectCard("Reading chair", "Living room", Icons.Default.Person, OneMint) }
            }
        }
        HouseholdStatusCard(homeSnapshot)
    }
}

@Composable
private fun HomeCameraStatusCard(
    paused: Boolean,
    cameras: List<OneCamera>?,
    loadState: OneCameraLoadState,
    loadError: String?,
    onRetry: () -> Unit,
    onOpenCamera: (OneCamera) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Camera and room setup", style = MaterialTheme.typography.titleMedium)
            Text(
                if (paused) "Camera capture is paused until the household enables room-data consent."
                else "Camera viewing is local and consent-based.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                else -> cameras.orEmpty().take(3).forEach { camera -> CameraStatusRow(camera, paused, onOpenCamera) }
            }
        }
    }
}

@Composable
private fun CameraStatusRow(camera: OneCamera, paused: Boolean, onOpenCamera: (OneCamera) -> Unit) {
    val status = if (paused) "Paused" else camera.status.cameraStatusLabel(camera.enabled)
    val tint = if (paused) OneAmber else camera.status.cameraStatusTint(camera.enabled)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(enabled = camera.enabled && !paused) { onOpenCamera(camera) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Visibility, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(camera.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${camera.roomId?.let { "Room configured" } ?: "Room not assigned"} · ${camera.platform}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.size(9.dp).background(tint, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(status, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
        if (camera.enabled && !paused) {
            Spacer(Modifier.width(7.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open camera", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

private fun String.cameraStatusLabel(enabled: Boolean): String = when {
    !enabled -> "Offline"
    equals("online", ignoreCase = true) -> "Online"
    equals("paused", ignoreCase = true) -> "Paused"
    equals("offline", ignoreCase = true) -> "Offline"
    else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun String.cameraStatusTint(enabled: Boolean): Color = when {
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
        ScreenHeader("CAMERA", camera.name, "Consent-based local view · receive only")
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
private fun CameraHeroCard() {
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
                    Text("LIVING ROOM CAMERA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(76.dp).align(Alignment.CenterHorizontally))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A steady view of the room", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
        modifier = Modifier.width(188.dp),
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
private fun HouseholdStatusCard(homeSnapshot: OneHomeSnapshot?) {
    val status = when {
        homeSnapshot == null -> "All connected"
        homeSnapshot.profile.paused -> "Camera capture paused"
        else -> "${homeSnapshot.objects.size} objects · ${homeSnapshot.events.size} events"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
            StatusRow("Household status", status, Icons.Default.CheckCircle, OneMint)
            HorizontalDivider()
            StatusRow("This week's plan", "3 check-ins · 1 review", Icons.Default.Schedule, OneBlue)
        }
    }
}

@Composable
private fun StatusRow(title: String, subtitle: String, icon: ImageVector, tint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun MapScreen(
    objects: List<OneRemoteObject>?,
    events: List<OneEvent>?,
    isBackend: Boolean,
    loadState: OneHomeLoadState,
    loadError: String?,
    onRetry: () -> Unit
) {
    ScreenScroll {
        ScreenHeader("MAP", "Home map", "Approximate locations · local view")
        when {
            isBackend && objects == null && (loadState == OneHomeLoadState.IDLE || loadState == OneHomeLoadState.LOADING) -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading the approximate map…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            isBackend && objects == null && loadState == OneHomeLoadState.ERROR -> {
                InfoCard("Map data unavailable", loadError ?: "ONE could not load the household map.")
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            else -> MapCanvas(objects = objects, isBackend = isBackend)
        }
        SectionHeading("EVIDENCE", "Recent observations")
        val visibleEvents = events ?: if (isBackend) emptyList() else demoEvents
        if (visibleEvents.isEmpty()) {
            InfoCard("No map observations yet", "Approximate evidence will appear here when ONE receives a consented observation.")
        } else {
            visibleEvents.take(2).forEach { event -> EventRow(event) }
        }
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Refresh room map")
        }
    }
}

@Composable
private fun MapCanvas(objects: List<OneRemoteObject>?, isBackend: Boolean) {
    if (!isBackend) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFB3E4EA), Color(0xFFEAF1E8))))
                .border(2.dp, OneBlue.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
        ) {
            Text("LIVING ROOM", modifier = Modifier.align(Alignment.TopStart).padding(26.dp), style = MaterialTheme.typography.labelSmall, color = OneBlue, fontWeight = FontWeight.Bold)
            MapPin("Blue mug", OneCyan, Modifier.align(Alignment.BottomStart).padding(start = 50.dp, bottom = 55.dp))
            MapPin("Kitchen", OneBlue, Modifier.align(Alignment.BottomEnd).padding(end = 60.dp, bottom = 100.dp))
            Text("Pins are approximate and include confidence", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val positionedObjects = objects.orEmpty().filter { it.pointX != null && it.pointY != null }
    if (positionedObjects.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("No approximate positions are available yet.", modifier = Modifier.padding(22.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val minX = positionedObjects.minOf { it.pointX!! }
    val maxX = positionedObjects.maxOf { it.pointX!! }
    val minY = positionedObjects.minOf { it.pointY!! }
    val maxY = positionedObjects.maxOf { it.pointY!! }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFB3E4EA), Color(0xFFEAF1E8))))
            .border(2.dp, OneBlue.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
    ) {
        Text("APPROXIMATE HOME MAP", modifier = Modifier.align(Alignment.TopStart).padding(26.dp), style = MaterialTheme.typography.labelSmall, color = OneBlue, fontWeight = FontWeight.Bold)
        positionedObjects.forEach { remoteObject ->
            val horizontal = normaliseMapCoordinate(remoteObject.pointX!!, minX, maxX)
            val vertical = 1f - normaliseMapCoordinate(remoteObject.pointY!!, minY, maxY)
            MapPin(
                remoteObject.label,
                OneCyan,
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxWidth * horizontal - 26.dp, y = maxHeight * vertical - 42.dp)
            )
        }
        Text("Pins are approximate and include confidence", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun normaliseMapCoordinate(value: Double, minimum: Double, maximum: Double): Float =
    if (minimum == maximum) 0.5f else ((value - minimum) / (maximum - minimum)).toFloat().coerceIn(0.12f, 0.88f)

@Composable
private fun MapPin(label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(30.dp).background(tint, CircleShape).border(3.dp, Color.White, CircleShape))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
            Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FamilyScreen(
    members: List<OneFamilyMember>?,
    isBackend: Boolean,
    loadState: OneFamilyLoadState,
    loadError: String?,
    onRetry: () -> Unit,
    medicationDoses: List<MedicationDose>?,
    medicationLoadState: OneMedicationLoadState,
    medicationLoadError: String?,
    onMedicationRetry: () -> Unit,
    medicationActionKey: String?,
    medicationActionError: String?,
    onMedicationStatusChange: (MedicationDose, DoseStatus) -> Unit
) {
    ScreenScroll {
        ScreenHeader("CARE CIRCLE", "Family, in sync.", "People, reminders, and permissions around the home.")
        AssistChip(onClick = { }, label = { Text("Everyone") }, leadingIcon = { Icon(Icons.Default.People, contentDescription = null) })
        Text("Showing plans and observations for Everyone. Switch people before reviewing sensitive details.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionHeading("PEOPLE", "Your care circle")
        when {
            !isBackend -> DemoFamilyMembersCard()
            members == null && (loadState == OneFamilyLoadState.IDLE || loadState == OneFamilyLoadState.LOADING) -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading the care circle…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            members == null && loadState == OneFamilyLoadState.ERROR -> {
                InfoCard("Family data unavailable", loadError ?: "ONE could not load the care circle.")
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            members.isNullOrEmpty() -> InfoCard("No family members recorded yet", "Invite a trusted person from the care circle when family sharing is enabled.")
            else -> {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        members.forEachIndexed { index, member ->
                            if (index > 0) HorizontalDivider()
                            CaregiverRow(
                                name = member.displayName,
                                relationship = member.email ?: "ONE member",
                                role = member.familyRoleLabel(),
                                tint = member.familyTint()
                            )
                        }
                    }
                }
            }
        }
        SectionHeading("TODAY'S PLAN", "Medication reminders")
        if (!isBackend) {
            demoMedicationDoses.forEach { dose -> MedicationRow(dose) }
        } else when {
            medicationDoses == null && (medicationLoadState == OneMedicationLoadState.IDLE || medicationLoadState == OneMedicationLoadState.LOADING) -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading medication reminders…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            medicationDoses == null && medicationLoadState == OneMedicationLoadState.ERROR -> {
                InfoCard("Medication data unavailable", medicationLoadError ?: "ONE could not load medication reminders.")
                OutlinedButton(onClick = onMedicationRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
            medicationDoses.isNullOrEmpty() -> InfoCard("No reminders for today", "No active medication reminder has been scheduled for this household today.")
            else -> {
                medicationActionError?.let { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                medicationDoses.forEach { dose ->
                    MedicationRow(
                        dose = dose,
                        actionKey = medicationActionKey,
                        onStatusChange = { status -> onMedicationStatusChange(dose, status) }
                    )
                }
            }
        }
        Text("Reminders support organization only. Confirm medication decisions with the resident and their care team.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DemoFamilyMembersCard() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            CaregiverRow("Biel Martínez", "You", "Owner", OneBlue)
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
private fun CaregiverRow(name: String, relationship: String, role: String, tint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(tint.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text(name.first().toString(), style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(relationship, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.12f)) {
            Text(role, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
        }
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
    eventStreamState: OneEventStreamState,
    eventStreamError: String?,
    clips: List<OneClip>?,
    clipLoadState: OneClipLoadState,
    clipLoadError: String?,
    onRetry: () -> Unit,
    onClipRetry: () -> Unit,
    onOpenEvent: (OneEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("EVENTS", "Reviewable moments", "A human-readable record of observed activity.") }
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
        val clipEventIds = clips.orEmpty().map { it.eventId }.toSet()
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
            else -> items(events) { event ->
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
            .clickable(enabled = onClick != null) { onClick?.invoke() },
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
        ScreenHeader("EVENT REVIEW", event.kind.label, "A human-readable record for caregiver attention.")
        EventRow(event, hasClip = !linkedClips.isNullOrEmpty())
        SectionHeading("CONTEXT", "What ONE observed")
        InfoCard("Approximate location", event.location)
        InfoCard("When it happened", event.time)
        InfoCard("Explanation", event.explanation)
        InfoCard("Confidence", event.confidence)
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
private fun ClipPlayer(apiClient: OneApiClient, session: OneSession, clip: OneClip) {
    val context = LocalContext.current
    var playbackState by remember(clip.id, session.accessToken) { mutableStateOf(Player.STATE_IDLE) }
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
private fun ResidentTodayScreen(onOpenAssistant: () -> Unit) {
    ScreenScroll {
        ScreenHeader("TODAY", "A more independent day.", "A little support, right when you need it.")
        Spacer(Modifier.height(32.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OneMint, modifier = Modifier.size(38.dp))
                Text("Your morning check-in is ready.", style = MaterialTheme.typography.titleLarge)
                Text("ONE can help you remember what comes next, at your own pace.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(52.dp))
        Button(onClick = onOpenAssistant, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = OneBlue)) {
            Icon(Icons.Default.GraphicEq, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text("Start check-in", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AssistantScreen(
    isBackend: Boolean,
    loadState: OneAssistantLoadState,
    result: OneCheckInResult?,
    loadError: String?,
    onSubmit: (String) -> Unit
) {
    var transcript by rememberSaveable { mutableStateOf("") }

    ScreenScroll {
        ScreenHeader("ASSISTANT", "I'm here with you.", "A calm daily check-in, one step at a time.")
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("Share a short note about how today is going. ONE will keep the check-in within the household context.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        result?.let { checkIn ->
            SectionHeading("LATEST CHECK-IN", "A human-readable summary")
            InfoCard("${checkIn.status.humanLabel()} · ${checkIn.trend.humanLabel()}", checkIn.explanation)
            if (checkIn.evidenceIds.isNotEmpty()) {
                Text("Based on ${checkIn.evidenceIds.size} recent household observation(s).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun AccountScreen(role: OneRole, onRoleChange: (OneRole) -> Unit, onSignOut: () -> Unit) {
    var roomConsent by rememberSaveable { mutableStateOf(true) }
    var microphoneConsent by rememberSaveable { mutableStateOf(true) }
    var clipsConsent by rememberSaveable { mutableStateOf(false) }

    ScreenScroll {
        ScreenHeader("ACCOUNT", "Privacy and control.", "Your home, your choices.")
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                ConsentRow("Room scan and map", roomConsent) { roomConsent = it }
                HorizontalDivider()
                ConsentRow("Microphone for push-to-talk", microphoneConsent) { microphoneConsent = it }
                HorizontalDivider()
                ConsentRow("Caregiver event clips", clipsConsent) { clipsConsent = it }
            }
        }
        Text("Sensitive room, audio, and clip data stays local unless you explicitly enable sharing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionHeading("DEMO", "Preview another experience")
        OutlinedButton(onClick = { onRoleChange(if (role == OneRole.CAREGIVER) OneRole.RESIDENT else OneRole.CAREGIVER) }, modifier = Modifier.fillMaxWidth()) {
            Icon(if (role == OneRole.CAREGIVER) Icons.Default.Person else Icons.Default.People, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (role == OneRole.CAREGIVER) "Preview resident experience" else "Preview caregiver experience")
        }
        SectionHeading("YOUR DATA", "Human control")
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Prepare a data export") }
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Request deletion") }
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Preview signed-out flow")
        }
        Text("Observations support human attention. They are not medical advice or a diagnosis.", style = MaterialTheme.typography.bodySmall, color = OneAmber)
    }
}

@Composable
private fun ConsentRow(label: String, enabled: Boolean, onChanged: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = enabled, onCheckedChange = onChanged)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OneAppPreview() {
    ONETheme { OneApp() }
}
