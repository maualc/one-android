package com.one.cognitivecompanion

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.BatteryManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.one.cognitivecompanion.ui.theme.OneAmber
import com.one.cognitivecompanion.ui.theme.OneBlue
import com.one.cognitivecompanion.ui.theme.OneCyan
import com.one.cognitivecompanion.ui.theme.OneMint
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

private enum class OutsideMapEditMode {
    NONE,
    SET_HOME,
    ADD_SAFE_PLACE,
    MOVE_SAFE_PLACE
}

private enum class OutsideDetailedMapMode {
    CONFIGURATION,
    HISTORY
}

private data class OutsideHistoryRange(
    val id: String,
    val label: String,
    val durationMillis: Long
)

private val outsideHistoryRanges = listOf(
    OutsideHistoryRange("1h", "1 hour", 60L * 60L * 1_000L),
    OutsideHistoryRange("6h", "6 hours", 6L * 60L * 60L * 1_000L),
    OutsideHistoryRange("24h", "24 hours", 24L * 60L * 60L * 1_000L),
    OutsideHistoryRange("3d", "3 days", 3L * 24L * 60L * 60L * 1_000L),
    OutsideHistoryRange("7d", "7 days", ONE_OUTSIDE_MAX_HISTORY_DAYS * 24L * 60L * 60L * 1_000L)
)

private const val OUTSIDE_DEFAULT_HISTORY_RANGE_ID = "24h"

private val outsideDemoRecipient = OneCareRecipient(
    id = UUID.nameUUIDFromBytes("one-outside-demo-recipient".toByteArray()),
    displayName = "Demo resident",
    relationship = "Local demo profile",
    roomLabel = null,
    createdAt = null
)

@Composable
fun OneOutsideTrackingScreen(
    careRecipients: List<OneCareRecipient>?,
    selectedCareRecipientId: UUID?,
    isBackend: Boolean,
    careRecipientsLoading: Boolean,
    onSelectCareRecipient: (UUID) -> Unit,
    onEnableOutsideConsent: (UUID) -> Unit,
    onOpenFamily: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { OneOutsideTrackingStore(context) }
    val locationRepository = remember(context) { OneLocationRepository(context) }
    val canClearSharedHistory = !isBackend || OneSecureStore(context).restore()?.session?.backendRole == "admin"
    val coroutineScope = rememberCoroutineScope()
    val availableRecipients = if (!isBackend && careRecipients.isNullOrEmpty()) {
        listOf(outsideDemoRecipient)
    } else {
        careRecipients.orEmpty()
    }
    var selectedPersonId by remember { mutableStateOf(selectedCareRecipientId ?: store.readSelectedPersonId()) }
    var followCurrentLocation by remember(selectedPersonId) { mutableStateOf(true) }
    var snapshot by remember { mutableStateOf<OneOutsideTrackingSnapshot?>(null) }
    var serviceError by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshingHistory by remember { mutableStateOf(false) }
    var pendingTrackingPersonId by remember { mutableStateOf<UUID?>(null) }
    var showBackgroundPermissionDialog by remember { mutableStateOf(false) }
    var hasBackgroundPermission by remember { mutableStateOf(hasOutsideBackgroundLocationPermission(context)) }
    var showRecipientMenu by remember { mutableStateOf(false) }
    var mapEditModeName by rememberSaveable { mutableStateOf(OutsideMapEditMode.NONE.name) }
    var movingSafePlaceId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPlacePoint by remember { mutableStateOf<OneExteriorPoint?>(null) }
    var editingPlaceId by remember { mutableStateOf<String?>(null) }
    var placeName by rememberSaveable { mutableStateOf("") }
    var placeRadius by rememberSaveable { mutableStateOf(ONE_OUTSIDE_DEFAULT_SAFE_RADIUS_METERS.toDisplayRadius()) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var showHomeRadiusDialog by remember { mutableStateOf(false) }
    var homeRadius by rememberSaveable { mutableStateOf(ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS.toDisplayRadius()) }
    var routeId by rememberSaveable { mutableStateOf(ONE_EXTERIOR_ROUTE_NEIGHBORHOOD) }
    var routeIndex by rememberSaveable { mutableIntStateOf(0) }
    var simulationRunning by rememberSaveable { mutableStateOf(false) }
    var routeMenuExpanded by remember { mutableStateOf(false) }
    var routeOriginKey by rememberSaveable { mutableStateOf(ONE_EXTERIOR_ROUTE_ORIGIN_HOME) }
    var routeDestinationId by rememberSaveable { mutableStateOf("") }
    var routeOriginMenuExpanded by remember { mutableStateOf(false) }
    var routeDestinationMenuExpanded by remember { mutableStateOf(false) }
    var messageDraft by rememberSaveable { mutableStateOf("") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showFullScreenMap by rememberSaveable { mutableStateOf(false) }
    var showAdvancedPanel by rememberSaveable { mutableStateOf(false) }
    var detailedMapModeName by rememberSaveable { mutableStateOf(OutsideDetailedMapMode.CONFIGURATION.name) }
    var historyRangeId by rememberSaveable { mutableStateOf(OUTSIDE_DEFAULT_HISTORY_RANGE_ID) }
    var mapSearchQuery by rememberSaveable { mutableStateOf("") }
    var submittedMapSearchQuery by remember { mutableStateOf("") }
    var mapSearchRequestId by remember { mutableIntStateOf(0) }
    var mapSearchResults by remember { mutableStateOf<List<OneOutsideLocationSearchResult>>(emptyList()) }
    var mapSearchError by rememberSaveable { mutableStateOf<String?>(null) }
    var mapSearchLoading by remember { mutableStateOf(false) }
    var mapSearchPoint by remember { mutableStateOf<OneExteriorPoint?>(null) }
    var mapCameraCommandId by rememberSaveable { mutableLongStateOf(0L) }
    var mapCameraCommand by remember { mutableStateOf<OneExteriorMapCameraCommand?>(null) }
    val mapEditMode = runCatching { OutsideMapEditMode.valueOf(mapEditModeName) }
        .getOrDefault(OutsideMapEditMode.NONE)
    val detailedMapMode = runCatching { OutsideDetailedMapMode.valueOf(detailedMapModeName) }
        .getOrDefault(OutsideDetailedMapMode.CONFIGURATION)
    val recipientIds = availableRecipients.map { it.id }
    val selectedRecipient = availableRecipients.firstOrNull { it.id == selectedPersonId }
    val selectedName = selectedRecipient?.displayName ?: "Person cared for"
    val selectedPhotoUri = snapshot?.profilePhotoUri ?: selectedRecipient?.profilePhotoUrl

    fun issueMapCameraCommand(action: OneExteriorMapCameraAction, target: OneExteriorPoint? = null) {
        mapCameraCommandId += 1L
        mapCameraCommand = OneExteriorMapCameraCommand(mapCameraCommandId, action, target)
    }

    fun submitMapSearch() {
        val query = mapSearchQuery.trim()
        if (query.isEmpty()) {
            mapSearchResults = emptyList()
            mapSearchError = "Escribe una dirección, un lugar o una ciudad."
            return
        }
        submittedMapSearchQuery = query
        mapSearchRequestId += 1
        mapSearchResults = emptyList()
        mapSearchError = null
    }

    fun selectMapSearchResult(result: OneOutsideLocationSearchResult) {
        mapSearchQuery = result.displayName
        mapSearchPoint = result.point
        mapSearchResults = emptyList()
        mapSearchError = null
        issueMapCameraCommand(OneExteriorMapCameraAction.FOCUS, result.point)
    }

    LaunchedEffect(mapSearchRequestId) {
        if (mapSearchRequestId == 0) return@LaunchedEffect
        val query = submittedMapSearchQuery
        mapSearchLoading = true
        mapSearchError = null
        try {
            if (!isBackend) throw IllegalStateException("Search needs the ONE API")
            val session = OneSecureStore(context).restore()?.session
                ?: throw IllegalStateException("Sign in to search for a place")
            val recipientId = selectedPersonId ?: throw IllegalStateException("Select a person")
            val results = searchOneOutsideLocations(query, session, recipientId)
            mapSearchResults = results
            if (results.isEmpty()) {
                mapSearchError = "No se han encontrado resultados para esa búsqueda."
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            mapSearchResults = emptyList()
            mapSearchError = if (!isBackend) "La búsqueda de lugares requiere conectarse a ONE."
                else "No se ha podido buscar la ubicación. Comprueba la conexión e inténtalo de nuevo."
        } finally {
            mapSearchLoading = false
        }
    }

    fun reload() {
        selectedPersonId?.let { snapshot = store.read(it) }
        hasBackgroundPermission = hasOutsideBackgroundLocationPermission(context)
    }

    fun syncZonesIfTracking(personId: UUID) {
        if (store.read(personId).trackingEnabled) {
            OneOutsideLocationService.syncGeofences(context, personId)
        }
    }

    fun clearMapEditMode() {
        mapEditModeName = OutsideMapEditMode.NONE.name
        movingSafePlaceId = null
    }

    fun setSelectedPerson(personId: UUID) {
        if (personId == selectedPersonId) {
            showRecipientMenu = false
            return
        }
        selectedPersonId?.let { previous ->
            if (store.read(previous).trackingEnabled) {
                store.setTrackingEnabled(previous, false)
                OneOutsideLocationService.stop(context)
            }
        }
        store.saveSelectedPersonId(personId)
        selectedPersonId = personId
        followCurrentLocation = true
        onSelectCareRecipient(personId)
        simulationRunning = false
        routeIndex = 0
        showRecipientMenu = false
        clearMapEditMode()
    }

    fun startGps(personId: UUID) {
        if (!hasOutsideLocationPermission(context)) {
            serviceError = "Location permission is required to record this phone's position."
            return
        }
        simulationRunning = false
        if (isBackend) onEnableOutsideConsent(personId)
        routeIndex = 0
        store.setTrackingEnabled(personId, true, OneOutsideTrackingMode.GPS)
        store.saveSelectedPersonId(personId)
        OneOutsideLocationService.start(context, personId)
        if (isBackend) coroutineScope.launch {
            serviceError = locationRepository.setDeviceStatus(personId, true).error
        }
        serviceError = null
        reload()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasOutsideBackgroundLocationPermission(context)
        ) {
            showBackgroundPermissionDialog = true
        }
    }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasOutsideLocationPermission(context)
        val personId = pendingTrackingPersonId
        pendingTrackingPersonId = null
        if (granted && personId != null) {
            startGps(personId)
        } else {
            serviceError = "Location permission is required to record this phone's position."
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { reload() }

    val backgroundSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { reload() }

    fun requestBackgroundLocation() {
        showBackgroundPermissionDialog = false
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            backgroundSettingsLauncher.launch(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = ("package:" + context.packageName).toUri()
                }
            )
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) serviceError = "Notifications are disabled; in-app alerts will still be kept in the local history."
    }

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val personId = selectedPersonId ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        store.saveProfilePhotoUri(personId, uri.toString())
        reload()
    }

    fun requestGps(personId: UUID) {
        if (!hasOutsideLocationPermission(context)) {
            pendingTrackingPersonId = personId
            foregroundLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        startGps(personId)
    }

    fun stopGps(personId: UUID) {
        store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.GPS)
        OneOutsideLocationService.stop(context)
        if (isBackend) coroutineScope.launch {
            serviceError = locationRepository.setDeviceStatus(personId, false).error
        }
        serviceError = null
        reload()
    }

    LaunchedEffect(recipientIds, selectedCareRecipientId, isBackend, careRecipientsLoading) {
        if (isBackend && (careRecipients == null || careRecipientsLoading)) return@LaunchedEffect
        val current = store.readSelectedPersonId()
        val preferred = selectedCareRecipientId ?: current
        val valid = preferred?.takeIf { it in recipientIds } ?: recipientIds.firstOrNull()
        if (valid != selectedPersonId) {
            selectedPersonId?.let { previous ->
                if (store.read(previous).trackingEnabled) {
                    store.setTrackingEnabled(previous, false)
                    OneOutsideLocationService.stop(context)
                }
            }
            store.saveSelectedPersonId(valid)
        }
        selectedPersonId = valid
        if (isBackend && valid != null && valid != selectedCareRecipientId) {
            onSelectCareRecipient(valid)
        }
    }

    fun saveHomeZone(personId: UUID, point: OneExteriorPoint, radius: Double) {
        store.saveHome(personId, point, radius)
        if (isBackend) store.queuePlace(personId, "home", "home", "save")
        if (isBackend) coroutineScope.launch {
            serviceError = locationRepository.saveHome(personId, point, radius).error
            reload()
        }
        syncZonesIfTracking(personId)
        reload()
    }

    LaunchedEffect(selectedPersonId) {
        simulationRunning = false
        routeIndex = 0
        snapshot = selectedPersonId?.let { store.read(it) }
    }

    LaunchedEffect(selectedPersonId, isBackend) {
        val personId = selectedPersonId ?: return@LaunchedEffect
        if (!isBackend) return@LaunchedEffect
        serviceError = locationRepository.pull(personId).error
        snapshot = store.read(personId)
        while (isActive) {
            val result = locationRepository.synchronize(personId)
            snapshot = store.read(personId)
            serviceError = result.error
            delay(30_000L)
        }
    }

    DisposableEffect(context, selectedPersonId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val id = intent.getStringExtra(OneOutsideLocationService.EXTRA_PERSON_ID)
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (id != null && id == selectedPersonId) {
                    snapshot = store.read(id)
                    intent.getStringExtra(OneOutsideLocationService.EXTRA_ERROR)?.let { error ->
                        serviceError = error
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(OneOutsideLocationService.ACTION_TRACKING_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(selectedPersonId) {
        while (isActive) {
            delay(30_000L)
            snapshot = selectedPersonId?.let { store.read(it) }
        }
    }

    LaunchedEffect(snapshot?.residentMessage, selectedPersonId) {
        messageDraft = snapshot?.residentMessage.orEmpty()
    }

    val currentPoint = snapshot?.lastPoint?.point
    val latestReliablePoint = snapshot?.points.orEmpty().lastOrNull { it.zoneKey != null }
    val selectedHistoryRange = outsideHistoryRanges.firstOrNull { it.id == historyRangeId }
        ?: outsideHistoryRanges.last()
    val historyCutoffMillis = System.currentTimeMillis() - selectedHistoryRange.durationMillis
    val historyPoints = snapshot?.points.orEmpty().filter { it.capturedAtMillis >= historyCutoffMillis }
    val routeOriginOptions = oneExteriorRouteOriginOptions(
        home = snapshot?.home,
        currentPoint = latestReliablePoint?.point ?: currentPoint,
        safePlaces = snapshot?.safePlaces.orEmpty()
    )
    val selectedRouteOrigin = routeOriginOptions.firstOrNull { it.key == routeOriginKey }
        ?: routeOriginOptions.firstOrNull()
    val routeDestinationOptions = snapshot?.safePlaces.orEmpty().filter { place ->
        place.id != selectedRouteOrigin?.safePlaceId
    }
    val selectedRouteDestination = routeDestinationOptions.firstOrNull { it.id == routeDestinationId }
        ?: routeDestinationOptions.firstOrNull()
    val routePoints = snapshot?.home?.let { home ->
        if (routeId == ONE_EXTERIOR_ROUTE_SAFE_PLACE && selectedRouteDestination == null) {
            emptyList()
        } else {
            oneOutsideRoutePoints(
                routeId = routeId,
                home = home,
                safePlaces = snapshot?.safePlaces.orEmpty(),
                originPoint = if (routeId == ONE_EXTERIOR_ROUTE_SAFE_PLACE) {
                    selectedRouteOrigin?.point ?: home.center
                } else {
                    home.center
                },
                destinationPoint = if (routeId == ONE_EXTERIOR_ROUTE_SAFE_PLACE) {
                    selectedRouteDestination?.center
                } else {
                    null
                }
            )
        }
    }.orEmpty()
    // Recorded GPS samples are observations, not a street route. Draw them as
    // points until a reliable pedestrian map-matching geometry is available.
    val detailedMapPoints = emptyList<OneExteriorPoint>()
    val detailedMapCurrentPoint = if (detailedMapMode == OutsideDetailedMapMode.HISTORY) {
        historyPoints.lastOrNull()?.point
    } else {
        currentPoint
    }
    val canAdvance = simulationRunning && routePoints.isNotEmpty() && routeIndex < routePoints.lastIndex
    val currentZone = snapshot?.currentZone
    val locationEnabled = isSystemLocationEnabled(context)
    val history = historyPoints.asReversed()

    fun resetRouteProgress() {
        simulationRunning = false
        routeIndex = 0
    }

    fun selectRouteOrigin(endpoint: OneExteriorRouteEndpoint) {
        routeOriginKey = endpoint.key
        routeDestinationId = snapshot?.safePlaces.orEmpty().firstOrNull { place ->
            place.id != endpoint.safePlaceId
        }?.id.orEmpty()
        resetRouteProgress()
        routeOriginMenuExpanded = false
    }

    fun selectRouteDestination(place: OneExteriorSafePlace) {
        routeDestinationId = place.id
        resetRouteProgress()
        routeDestinationMenuExpanded = false
    }

    fun recenterMap() {
        val target = latestReliablePoint?.point ?: currentPoint ?: snapshot?.home?.center
        if (target == null) {
            mapSearchError = "Todavía no hay una ubicación disponible para recentrar el mapa."
            return
        }
        mapSearchPoint = null
        mapSearchResults = emptyList()
        mapSearchError = null
        followCurrentLocation = true
        issueMapCameraCommand(OneExteriorMapCameraAction.RECENTER, target)
    }

    fun openFullScreenMap(mode: OutsideDetailedMapMode = OutsideDetailedMapMode.CONFIGURATION) {
        // A new MapView is created for the dialog. The current focus point is
        // enough to restore the useful camera target without replaying an old
        // zoom command on the new instance.
        mapCameraCommand = null
        detailedMapModeName = mode.name
        showFullScreenMap = true
    }

    fun openHistoryMap() {
        openFullScreenMap(OutsideDetailedMapMode.HISTORY)
        if (isBackend) selectedPersonId?.let { personId -> coroutineScope.launch {
            refreshingHistory = true
            try { serviceError = locationRepository.pull(personId).error; reload() }
            finally { refreshingHistory = false }
        } }
    }

    fun closeFullScreenMap() {
        mapCameraCommand = null
        showFullScreenMap = false
        if (detailedMapMode == OutsideDetailedMapMode.CONFIGURATION && mapEditMode != OutsideMapEditMode.NONE) {
            clearMapEditMode()
        }
    }

    fun openMapEditor(mode: OutsideMapEditMode, safePlaceId: String? = null) {
        mapEditModeName = mode.name
        movingSafePlaceId = safePlaceId
        openFullScreenMap(OutsideDetailedMapMode.CONFIGURATION)
    }

    fun handleMapTap(point: OneExteriorPoint) {
        val personId = selectedPersonId ?: return
        when (mapEditMode) {
            OutsideMapEditMode.SET_HOME -> {
                val radius = snapshot?.home?.radiusMeters ?: ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS
                saveHomeZone(personId, point, radius)
                clearMapEditMode()
                syncZonesIfTracking(personId)
                reload()
            }
            OutsideMapEditMode.ADD_SAFE_PLACE -> {
                pendingPlacePoint = point
                editingPlaceId = null
                placeName = ""
                placeRadius = ONE_OUTSIDE_DEFAULT_SAFE_RADIUS_METERS.toDisplayRadius()
                showPlaceDialog = true
            }
            OutsideMapEditMode.MOVE_SAFE_PLACE -> {
                val place = snapshot?.safePlaces.orEmpty().firstOrNull { it.id == movingSafePlaceId }
                if (place != null) {
                    store.saveSafePlace(personId, place.copy(center = point))
                    if (isBackend) store.queuePlace(personId, place.id, "safe", "save")
                    if (isBackend) coroutineScope.launch {
                        serviceError = locationRepository.saveSafePlace(personId, place.copy(center = point)).error
                        reload()
                    }
                    syncZonesIfTracking(personId)
                }
                clearMapEditMode()
                reload()
            }
            OutsideMapEditMode.NONE -> Unit
        }
    }

    fun advanceSimulation() {
        val personId = selectedPersonId ?: return
        val point = routePoints.getOrNull(routeIndex) ?: return
        store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.SIMULATED)
        store.appendLocation(
            personId = personId,
            point = point,
            accuracyMeters = 8f,
            source = OneOutsideLocationSource.SIMULATED
        )
        routeIndex = (routeIndex + 1).coerceAtMost(routePoints.lastIndex)
        if (routeIndex >= routePoints.lastIndex) simulationRunning = false
        reload()
    }

    fun startSimulation() {
        if (snapshot?.home == null || routePoints.isEmpty()) return
        val personId = selectedPersonId ?: return
        store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.SIMULATED)
        OneOutsideLocationService.stop(context)
        routeIndex = 0
        simulationRunning = true
        advanceSimulation()
        reload()
    }

    LaunchedEffect(simulationRunning, routeId, selectedPersonId, snapshot?.home, snapshot?.safePlaces) {
        while (simulationRunning) {
            delay(60_000L)
            advanceSimulation()
        }
    }

    if (availableRecipients.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutsideHeader("LOCATION", "Outside companion", "Track one local phone with places, history and clear alerts.")
                if (careRecipientsLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Loading people cared for…", style = MaterialTheme.typography.bodySmall)
                } else {
                    OutsideInfoCard("Add a person first", "Create a person in Family before assigning this phone to their local tracking profile.")
                    OutlinedButton(onClick = onOpenFamily, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.People, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Family")
                    }
                }
            }
        }
        return
    }

    if (!showAdvancedPanel) {
        Box(modifier = Modifier.fillMaxSize()) {
            OneExteriorMap(
                home = snapshot?.home,
                safePlaces = snapshot?.safePlaces.orEmpty(),
                routePoints = emptyList(),
                currentPoint = currentPoint,
                focusPoint = latestReliablePoint?.point ?: currentPoint ?: snapshot?.home?.center,
                cameraCommand = mapCameraCommand,
                onMapTap = {},
                personName = selectedName,
                personPhotoUri = selectedPhotoUri,
                followCurrentLocation = followCurrentLocation,
                onUserMapMove = { followCurrentLocation = false },
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 8.dp
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutsideProfileAvatar(
                            name = selectedName,
                            photoUri = selectedPhotoUri,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                currentZone?.label ?: if (currentPoint == null) "Waiting for location" else "Outside saved places",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentZone != null) OneMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (availableRecipients.size > 1) {
                            TextButton(onClick = { showRecipientMenu = true }) {
                                Text("Change")
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showRecipientMenu,
                        onDismissRequest = { showRecipientMenu = false },
                        modifier = Modifier.width(260.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        availableRecipients.forEach { recipient ->
                            DropdownMenuItem(
                                text = { Text(recipient.displayName) },
                                onClick = { setSelectedPerson(recipient.id) }
                            )
                        }
                    }
                }
            }

            OutsideMapControls(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                onZoomIn = { issueMapCameraCommand(OneExteriorMapCameraAction.ZOOM_IN) },
                onZoomOut = { issueMapCameraCommand(OneExteriorMapCameraAction.ZOOM_OUT) },
                onRecenter = ::recenterMap
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                snapshot?.lastPoint?.let {
                                    oneOutsideLocationLabel(it, snapshot?.home, snapshot?.safePlaces.orEmpty())
                                } ?: "Location not available yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                latestReliablePoint?.let { point ->
                                    val dwell = point.dwellDurationMillis
                                    if (dwell >= 60_000L) "Here for ${formatOutsideDuration(dwell)} · updated ${formatOutsideTime(point.capturedAtMillis)}"
                                    else "Updated ${formatOutsideTime(point.capturedAtMillis)}"
                                } ?: "Enable location sharing to place this phone on the map",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = snapshot?.trackingEnabled == true,
                            onCheckedChange = { enabled ->
                                val personId = selectedPersonId ?: return@Switch
                                if (enabled) requestGps(personId) else stopGps(personId)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            latestReliablePoint?.accuracyMeters?.let { "Accuracy ±${it.toInt()} m" } ?: "Accuracy —",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            readOutsideBatteryPercent(context)?.let { "Battery $it%" } ?: "Battery —",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (snapshot?.trackingEnabled == true) "Sharing on" else "Sharing paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (snapshot?.trackingEnabled == true) OneMint else OneAmber
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = ::openHistoryMap, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("History")
                        }
                        Button(onClick = { showAdvancedPanel = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Places")
                        }
                        IconButton(onClick = { profilePhotoLauncher.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Choose profile photo", tint = OneBlue)
                        }
                    }
                    if (isBackend) {
                        Text(
                            "Location saved on this phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = { showAdvancedPanel = false }) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Back to live map")
            }
            OutsideHeader(
                "LOCATION",
                "Outside companion",
                "A calm map for where the person is, where they have been and which places feel familiar."
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutsideProfileAvatar(
                            name = selectedName,
                            photoUri = selectedPhotoUri,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LOCAL PROFILE", style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
                            Text(selectedName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                selectedRecipient?.relationship ?: "This phone's outside companion profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { profilePhotoLauncher.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Choose profile photo", tint = OneBlue)
                        }
                        BoxWithContent(
                            expanded = showRecipientMenu,
                            onClick = { showRecipientMenu = true }
                        )
                    }
                    DropdownMenu(
                        expanded = showRecipientMenu,
                        onDismissRequest = { showRecipientMenu = false },
                        modifier = Modifier.width(260.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        availableRecipients.forEach { recipient ->
                            DropdownMenuItem(
                                text = { Text(recipient.displayName) },
                                onClick = { setSelectedPerson(recipient.id) }
                            )
                        }
                    }
                    Text(
                        if (isBackend) "Location is shared with this care space when tracking is enabled."
                        else "Demo profile: location data stays on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location sharing on this phone", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    snapshot?.trackingEnabled == true && !locationEnabled -> "Tracking is enabled, but Android location services are off."
                                    snapshot?.trackingEnabled == true -> "Active · updates are saved locally at a battery-aware pace."
                                    !isBackend -> "Paused · use Test a route to preview the map and alerts."
                                    else -> "Paused · enable tracking to record this phone's GPS history."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (snapshot?.trackingEnabled == true) OneMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = snapshot?.trackingEnabled == true,
                            onCheckedChange = { enabled ->
                                val personId = selectedPersonId ?: return@Switch
                                if (enabled) requestGps(personId) else stopGps(personId)
                            }
                        )
                    }
                    if (snapshot?.trackingEnabled == true && latestReliablePoint != null) {
                        Text(
                            "Last reliable point · " +
                                oneOutsideLocationLabel(
                                    latestReliablePoint,
                                    snapshot?.home,
                                    snapshot?.safePlaces.orEmpty()
                                ) +
                                " · " + formatOutsideTime(latestReliablePoint.capturedAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!locationEnabled) {
                        OutsideNotice(
                            title = "Location is off",
                            detail = "Enable Android location to record GPS.",
                            tint = OneAmber,
                            icon = Icons.Default.Warning,
                            action = {
                                OutlinedButton(onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }) { Text("Open location settings") }
                            }
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        snapshot?.trackingEnabled == true &&
                        !hasBackgroundPermission
                    ) {
                        OutsideNotice(
                            title = "Background access",
                            detail = "Choose “Allow all the time” for zone alerts.",
                            tint = OneAmber,
                            icon = Icons.Default.LocationOn,
                            action = {
                                OutlinedButton(onClick = { showBackgroundPermissionDialog = true }) {
                                    Text("Enable background access")
                                }
                            }
                        )
                    }
                    serviceError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        OutlinedButton(
                            onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Allow outside alerts")
                        }
                    }
                }
            }
        }
        item {
            OutsideLocationOverviewCard(
                personName = selectedName,
                profilePhotoUri = selectedPhotoUri,
                home = snapshot?.home,
                safePlaces = snapshot?.safePlaces.orEmpty(),
                points = snapshot?.points.orEmpty().takeLast(80),
                currentPoint = currentPoint,
                latestLabel = snapshot?.lastPoint?.let {
                    oneOutsideLocationLabel(it, snapshot?.home, snapshot?.safePlaces.orEmpty())
                },
                onOpenHistory = ::openHistoryMap,
                onConfigureMap = { openFullScreenMap(OutsideDetailedMapMode.CONFIGURATION) }
            )
        }
        if (mapEditMode != OutsideMapEditMode.NONE) {
            item {
                OutsideNotice(
                    title = when (mapEditMode) {
                        OutsideMapEditMode.SET_HOME -> "Tap the map to place home."
                        OutsideMapEditMode.ADD_SAFE_PLACE -> "Tap the map to place the safe place."
                        OutsideMapEditMode.MOVE_SAFE_PLACE -> "Tap the map to move the safe place."
                        OutsideMapEditMode.NONE -> ""
                    },
                    detail = "Tap the map to choose the point.",
                    tint = OneBlue,
                    icon = Icons.Default.Map,
                    action = { TextButton(onClick = ::clearMapEditMode) { Text("Cancel") } }
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Places and zones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = OneBlue)
                    }
                    Text(
                        "Set familiar places so ONE can explain departures, returns and arrivals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                openMapEditor(OutsideMapEditMode.SET_HOME)
                                showHomeRadiusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(if (snapshot?.home == null) "Set home" else "Move home")
                        }
                    }
                    latestReliablePoint?.let { point ->
                        OutlinedButton(
                            onClick = {
                                selectedPersonId?.let { personId ->
                                    saveHomeZone(
                                        personId,
                                        point.point,
                                        snapshot?.home?.radiusMeters ?: ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS
                                    )
                                    syncZonesIfTracking(personId)
                                    reload()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text(if (snapshot?.home == null) "Use latest reliable location as home" else "Move home to latest reliable location")
                        }
                    }
                    snapshot?.home?.let { home ->
                        OutsidePlaceRow(
                            icon = Icons.Default.Home,
                            name = "Home",
                            detail = "Radius · " + home.radiusMeters.toDisplayRadius() + " m",
                            onEdit = {
                                homeRadius = home.radiusMeters.toDisplayRadius()
                                showHomeRadiusDialog = true
                            },
                                onMove = {
                                    openMapEditor(OutsideMapEditMode.SET_HOME)
                                },
                            onDelete = {
                                selectedPersonId?.let { personId ->
                                    if (isBackend) store.queuePlace(personId, "home", "home", "delete")
                                    store.removeHome(personId)
                                    if (isBackend) coroutineScope.launch {
                                        serviceError = locationRepository.deleteHome(personId).error
                                        reload()
                                    }
                                    syncZonesIfTracking(personId)
                                    reload()
                                }
                            }
                        )
                    }
                    if (snapshot?.safePlaces.orEmpty().isEmpty()) {
                        Text("No safe places configured yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        snapshot?.safePlaces.orEmpty().forEach { place ->
                            OutsidePlaceRow(
                                icon = Icons.Default.LocationOn,
                                name = place.name,
                                detail = "Radius · " + place.radiusMeters.toDisplayRadius() + " m",
                                onEdit = {
                                    pendingPlacePoint = place.center
                                    editingPlaceId = place.id
                                    placeName = place.name
                                    placeRadius = place.radiusMeters.toDisplayRadius()
                                    showPlaceDialog = true
                                },
                                onMove = {
                                    openMapEditor(OutsideMapEditMode.MOVE_SAFE_PLACE, place.id)
                                },
                                onDelete = {
                                    selectedPersonId?.let { personId ->
                                        if (isBackend) store.queuePlace(personId, place.id, "safe", "delete")
                                        store.removeSafePlace(personId, place.id)
                                        if (isBackend) coroutineScope.launch {
                                            serviceError = locationRepository.deleteSafePlace(personId, place).error
                                            reload()
                                        }
                                        syncZonesIfTracking(personId)
                                        reload()
                                    }
                                }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            openMapEditor(OutsideMapEditMode.ADD_SAFE_PLACE)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Add safe place")
                    }
                }
            }
        }
        if (!isBackend) {
            item {
                Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Test a route", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OneBlue)
                    }
                    Text("Use a simulated walk to test the map and one alert per zone transition without moving outside.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { routeMenuExpanded = true }) {
                                Text(oneExteriorRouteTemplates.firstOrNull { it.id == routeId }?.label ?: "Neighbourhood walk")
                            }
                            Text(
                                oneExteriorRouteTemplates.firstOrNull { it.id == routeId }?.description.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = routeMenuExpanded,
                            onDismissRequest = { routeMenuExpanded = false },
                            modifier = Modifier.width(250.dp),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            oneExteriorRouteTemplates.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template.label) },
                                    onClick = {
                                        routeId = template.id
                                        resetRouteProgress()
                                        routeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (routeId == ONE_EXTERIOR_ROUTE_SAFE_PLACE) {
                        Text("Start from", style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
                        Box {
                            OutlinedButton(
                                onClick = { routeOriginMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = routeOriginOptions.isNotEmpty()
                            ) {
                                Text(selectedRouteOrigin?.label ?: "Choose a start point")
                            }
                            DropdownMenu(
                                expanded = routeOriginMenuExpanded,
                                onDismissRequest = { routeOriginMenuExpanded = false },
                                modifier = Modifier.width(260.dp),
                                shape = RoundedCornerShape(18.dp),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                routeOriginOptions.forEach { endpoint ->
                                    DropdownMenuItem(
                                        text = { Text(endpoint.label) },
                                        onClick = { selectRouteOrigin(endpoint) }
                                    )
                                }
                            }
                        }
                        Text("Arrive at", style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
                        Box {
                            OutlinedButton(
                                onClick = { routeDestinationMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = routeDestinationOptions.isNotEmpty()
                            ) {
                                Text(selectedRouteDestination?.name ?: "Choose a safe place")
                            }
                            DropdownMenu(
                                expanded = routeDestinationMenuExpanded,
                                onDismissRequest = { routeDestinationMenuExpanded = false },
                                modifier = Modifier.width(260.dp),
                                shape = RoundedCornerShape(18.dp),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                routeDestinationOptions.forEach { place ->
                                    DropdownMenuItem(
                                        text = { Text(place.name) },
                                        onClick = { selectRouteDestination(place) }
                                    )
                                }
                            }
                        }
                        if (routeDestinationOptions.isEmpty()) {
                            Text(
                                "Add at least one safe place different from the selected start point.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                (selectedRouteOrigin?.label ?: "Start") + " → " +
                                    (selectedRouteDestination?.name ?: "safe place"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (snapshot?.home == null) {
                        Text("Set home before starting a route.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        if (routePoints.isNotEmpty()) {
                            Text(
                                (currentZone?.label ?: "Ready") + " · step " +
                                    (routeIndex + 1).coerceAtMost(routePoints.size) + "/" + routePoints.size,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (routeId == ONE_EXTERIOR_ROUTE_SAFE_PLACE) {
                            Text(
                                "Choose a different safe place as destination to build this route.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = ::startSimulation,
                                enabled = !simulationRunning && routePoints.isNotEmpty()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text("Start")
                            }
                            OutlinedButton(onClick = ::advanceSimulation, enabled = canAdvance) {
                                Text("Advance")
                            }
                            TextButton(
                                onClick = {
                                    simulationRunning = false
                                    routeIndex = 0
                                    selectedPersonId?.let { personId ->
                                        store.appendLocation(
                                            personId,
                                            snapshot?.home?.center ?: return@let,
                                            8f,
                                            source = OneOutsideLocationSource.SIMULATED
                                        )
                                        reload()
                                    }
                                }
                            ) { Text("Reset") }
                        }
                        if (simulationRunning) {
                            Text("Auto advance runs every 60 seconds while this screen is open.", style = MaterialTheme.typography.labelSmall, color = OneMint)
                        }
                    }
                }
            }
        }
        }
        item {
            OutsideStatusCard(
                zone = currentZone,
                lastPoint = snapshot?.lastPoint,
                homeConfigured = snapshot?.home != null,
                home = snapshot?.home,
                safePlaces = snapshot?.safePlaces.orEmpty()
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Location history", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.History, contentDescription = null, tint = OneBlue)
                    }
                    Text(
                        "Last seen · ${selectedHistoryRange.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (history.isEmpty()) {
                        Text("No location recorded in this period.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val latestHistoryPoint = history.first()
                        Text(
                            oneOutsideLocationLabel(
                                latestHistoryPoint,
                                snapshot?.home,
                                snapshot?.safePlaces.orEmpty()
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatOutsideTime(latestHistoryPoint.capturedAtMillis)} · ${latestHistoryPoint.source.wireValue.uppercase(Locale.US)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        history.drop(1).take(2).forEach { point ->
                            OutsideHistoryRow(
                                point = point,
                                home = snapshot?.home,
                                safePlaces = snapshot?.safePlaces.orEmpty()
                            )
                        }
                        if (history.size > 3) {
                            Text(
                                "+${history.size - 3} more locations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(onClick = ::openHistoryMap, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Open full history")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val personId = selectedPersonId
                                if (isBackend && personId != null) coroutineScope.launch {
                                    refreshingHistory = true
                                    try { serviceError = locationRepository.pull(personId).error; reload() }
                                    finally { refreshingHistory = false }
                                } else reload()
                            },
                            enabled = !refreshingHistory,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(if (refreshingHistory) "Loading" else "Refresh")
                        }
                        if (canClearSharedHistory) OutlinedButton(
                            onClick = { showClearHistoryDialog = true },
                            enabled = history.isNotEmpty() || snapshot?.alerts?.isNotEmpty() == true,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Clear history")
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Outside alerts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = OneBlue)
                    }
                    if (snapshot?.alerts.orEmpty().isEmpty()) {
                        Text("No zone transitions recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        snapshot?.alerts.orEmpty().asReversed().take(5).forEach { alert ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(alert.title, style = MaterialTheme.typography.titleSmall)
                                Text(alert.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatOutsideTime(alert.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (alert != snapshot?.alerts.orEmpty().asReversed().take(5).last()) HorizontalDivider()
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Resident message preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OneBlue)
                    }
                    Text("This is a local test notification on this phone. It is not delivered to another family member.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it.take(240) },
                        label = { Text("Short message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Button(
                        onClick = {
                            val personId = selectedPersonId
                            val message = messageDraft.trim()
                            if (personId != null && message.isNotBlank()) {
                                store.setResidentMessage(personId, message)
                                OneOutsideNotificationHelper.notifyResidentMessage(context, selectedName, message)
                                reload()
                            }
                        },
                        enabled = messageDraft.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Show test notification")
                    }
                    if (snapshot?.residentMessage.orEmpty().isNotBlank()) {
                        OutsideInfoCard("Resident alert", snapshot?.residentMessage.orEmpty())
                    }
                }
            }
        }
        item {
            Text(
                "GPS is approximate; zone circles are not exact coordinates.",
                style = MaterialTheme.typography.bodySmall,
                color = OneAmber
            )
        }
    }

    if (showFullScreenMap) {
        OutsideFullScreenMapDialog(
            personName = selectedName,
            personPhotoUri = selectedPhotoUri,
            home = snapshot?.home,
            safePlaces = snapshot?.safePlaces.orEmpty(),
            routePoints = detailedMapPoints,
            currentPoint = detailedMapCurrentPoint,
            historyPoints = historyPoints,
            focusPoint = mapSearchPoint ?: detailedMapCurrentPoint,
            searchPoint = mapSearchPoint,
            cameraCommand = mapCameraCommand,
            onMapTap = ::handleMapTap,
            historyMode = detailedMapMode == OutsideDetailedMapMode.HISTORY,
            historyRanges = outsideHistoryRanges,
            selectedHistoryRangeId = selectedHistoryRange.id,
            onHistoryRangeSelected = { historyRangeId = it },
            searchQuery = mapSearchQuery,
            searchResults = mapSearchResults,
            searchLoading = mapSearchLoading,
            searchError = mapSearchError,
            onSearchQueryChange = {
                mapSearchQuery = it
                mapSearchResults = emptyList()
                mapSearchError = null
            },
            onSearch = ::submitMapSearch,
            onSelectSearchResult = ::selectMapSearchResult,
            onZoomIn = { issueMapCameraCommand(OneExteriorMapCameraAction.ZOOM_IN) },
            onZoomOut = { issueMapCameraCommand(OneExteriorMapCameraAction.ZOOM_OUT) },
            onRecenter = ::recenterMap,
            onDismiss = ::closeFullScreenMap
        )
    }

    if (showBackgroundPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundPermissionDialog = false },
            title = { Text("Keep location history while outside?") },
            text = {
                Text(
                    "ONE needs background location to keep recording this phone's position and " +
                        "detect safe-place arrivals and departures when the app is closed. " +
                        "Choose ‘Allow all the time’ in Android settings. " +
                        "The history stays on this phone for up to seven days. " +
                        "You can decline and keep tracking only while the app is in use."
                )
            },
            confirmButton = {
                TextButton(onClick = ::requestBackgroundLocation) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundPermissionDialog = false }) { Text("Not now") }
            }
        )
    }
    if (showPlaceDialog) {
        OutsideDialog(
            onDismissRequest = {
                showPlaceDialog = false
                pendingPlacePoint = null
                editingPlaceId = null
                clearMapEditMode()
            },
            title = if (editingPlaceId == null) "Name safe place" else "Edit safe place",
            onConfirm = {
                val point = pendingPlacePoint
                val personId = selectedPersonId
                if (point != null && personId != null) {
                    val radius = placeRadius.toDoubleOrNull()
                        ?.coerceIn(ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS, 1_000.0)
                        ?: ONE_OUTSIDE_DEFAULT_SAFE_RADIUS_METERS
                    val place = OneExteriorSafePlace(
                        id = editingPlaceId ?: UUID.randomUUID().toString(),
                        name = placeName.trim().ifBlank { "Safe place" },
                        center = point,
                        radiusMeters = radius
                    )
                    store.saveSafePlace(personId, place)
                    if (isBackend) store.queuePlace(personId, place.id, "safe", "save")
                    if (isBackend) coroutineScope.launch {
                        serviceError = locationRepository.saveSafePlace(personId, place).error
                        reload()
                    }
                    syncZonesIfTracking(personId)
                    showPlaceDialog = false
                    pendingPlacePoint = null
                    editingPlaceId = null
                    clearMapEditMode()
                    reload()
                }
            },
            confirmLabel = "Save",
            confirmEnabled = pendingPlacePoint != null,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = placeName,
                        onValueChange = { placeName = it.take(80) },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = placeRadius,
                        onValueChange = { placeRadius = it.filter { c -> c.isDigit() || c == '.' }.take(7) },
                        label = { Text("Radius in metres (20 minimum)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
    if (showHomeRadiusDialog) {
        OutsideDialog(
            onDismissRequest = { showHomeRadiusDialog = false },
            title = "Home radius",
            onConfirm = {
                val personId = selectedPersonId
                val home = snapshot?.home
                if (personId != null && home != null) {
                    val radius = homeRadius.toDoubleOrNull()
                        ?.coerceIn(ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS, 1_000.0)
                        ?: ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS
                    saveHomeZone(personId, home.center, radius)
                    syncZonesIfTracking(personId)
                    showHomeRadiusDialog = false
                    reload()
                }
            },
            confirmLabel = "Save",
            content = {
                OutlinedTextField(
                    value = homeRadius,
                    onValueChange = { homeRadius = it.filter { c -> c.isDigit() || c == '.' }.take(7) },
                    label = { Text("Radius in metres (20 minimum)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
    if (showClearHistoryDialog) {
        OutsideDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = if (isBackend) "Clear shared history?" else "Clear local history?",
            onConfirm = {
                selectedPersonId?.let { personId ->
                    if (isBackend) coroutineScope.launch {
                        serviceError = locationRepository.clearSharedHistory(personId).error
                        reload()
                    } else store.clearHistory(personId)
                }
                showClearHistoryDialog = false
                reload()
            },
            confirmLabel = "Clear",
            content = {
                Text(if (isBackend) "Permanently delete the shared location history for $selectedName? Other devices will remove their copies when they reconnect."
                    else "This removes the last 7 days of points and outside alerts for $selectedName from this phone.")
            }
        )
    }
}

@Composable
private fun OutsideProfileAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, photoUri) {
        value = photoUri?.let { value ->
            withContext(Dispatchers.IO) {
                runCatching {
                    if (value.startsWith("http://") || value.startsWith("https://")) {
                        val connection = (URL(value).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 10_000
                            readTimeout = 10_000
                            useCaches = true
                        }
                        try {
                            if (connection.responseCode in 200..299) {
                                connection.inputStream.use(BitmapFactory::decodeStream)
                            } else {
                                null
                            }
                        } finally {
                            connection.disconnect()
                        }
                    } else {
                        context.contentResolver.openInputStream(value.toUri())?.use(BitmapFactory::decodeStream)
                    }
                }.getOrNull()
            }
        }
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(OneBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "$name profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                outsideInitials(name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OneBlue
            )
        }
    }
}

@Composable
private fun OutsideLocationOverviewCard(
    personName: String,
    profilePhotoUri: String?,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    points: List<OneOutsideLocationPoint>,
    currentPoint: OneExteriorPoint?,
    latestLabel: String?,
    onOpenHistory: () -> Unit,
    onConfigureMap: () -> Unit
) {
    var followLocation by remember(personName) { mutableStateOf(true) }
    val pointsToPlot = buildList {
        addAll(points.map { it.point })
        currentPoint?.takeIf { it !in this }?.let(::add)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LOCATION OVERVIEW", style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
                    Text("Current trace", style = MaterialTheme.typography.titleMedium)
                    Text(
                        latestLabel?.let { "Latest point · $it" } ?: "Waiting for the first GPS point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    pointsToPlot.size.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = OneBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                OneExteriorMap(
                    home = home,
                    safePlaces = safePlaces,
                    routePoints = emptyList(),
                    historyPoints = pointsToPlot,
                    currentPoint = currentPoint,
                    focusPoint = currentPoint ?: pointsToPlot.lastOrNull(),
                    onMapTap = {},
                    personName = personName,
                    personPhotoUri = profilePhotoUri,
                    followCurrentLocation = followLocation,
                    onUserMapMove = { followLocation = false },
                    modifier = Modifier.fillMaxSize()
                )
                if (currentPoint != null) {
                    OutsideMapControlButton(
                        icon = Icons.Default.MyLocation,
                        contentDescription = "Center on person's location",
                        onClick = { followLocation = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    )
                }
                if (pointsToPlot.isEmpty()) {
                    Text(
                        "Waiting for GPS history",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "© OpenStreetMap · OpenFreeMap",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black
                )
            }
            Text(
                "Map overview · open History for GPS points and the time range.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("History")
                }
                OutlinedButton(onClick = onConfigureMap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Configure map")
                }
            }
        }
    }
}

@Composable
private fun OutsideMapSection(
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    routePoints: List<OneExteriorPoint>,
    currentPoint: OneExteriorPoint?,
    focusPoint: OneExteriorPoint?,
    searchPoint: OneExteriorPoint?,
    cameraCommand: OneExteriorMapCameraCommand?,
    onMapTap: (OneExteriorPoint) -> Unit,
    searchQuery: String,
    searchResults: List<OneOutsideLocationSearchResult>,
    searchLoading: Boolean,
    searchError: String?,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectSearchResult: (OneOutsideLocationSearchResult) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onOpenFullScreen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            OutsideMapSearchBar(
                query = searchQuery,
                results = searchResults,
                loading = searchLoading,
                error = searchError,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                onSelectResult = onSelectSearchResult,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutsideMapControlButton(
                icon = Icons.Default.Fullscreen,
                contentDescription = "Open map fullscreen",
                onClick = onOpenFullScreen
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            OneExteriorMap(
                home = home,
                safePlaces = safePlaces,
                routePoints = routePoints,
                currentPoint = currentPoint,
                focusPoint = focusPoint,
                searchPoint = searchPoint,
                cameraCommand = cameraCommand,
                onMapTap = onMapTap,
                modifier = Modifier.fillMaxSize()
            )
            OutsideMapControls(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onRecenter = onRecenter
            )
        }
    }
}

@Composable
private fun OutsideFullScreenMapDialog(
    personName: String,
    personPhotoUri: String?,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    routePoints: List<OneExteriorPoint>,
    currentPoint: OneExteriorPoint?,
    historyPoints: List<OneOutsideLocationPoint>,
    focusPoint: OneExteriorPoint?,
    searchPoint: OneExteriorPoint?,
    cameraCommand: OneExteriorMapCameraCommand?,
    onMapTap: (OneExteriorPoint) -> Unit,
    historyMode: Boolean,
    historyRanges: List<OutsideHistoryRange>,
    selectedHistoryRangeId: String,
    onHistoryRangeSelected: (String) -> Unit,
    searchQuery: String,
    searchResults: List<OneOutsideLocationSearchResult>,
    searchLoading: Boolean,
    searchError: String?,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectSearchResult: (OneOutsideLocationSearchResult) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OneExteriorMap(
                    home = home,
                    safePlaces = safePlaces,
                    routePoints = routePoints,
                    historyPoints = outsideHistoryPointsForMap(historyPoints),
                    currentPoint = currentPoint,
                    focusPoint = focusPoint,
                    searchPoint = searchPoint,
                    cameraCommand = cameraCommand,
                    onMapTap = onMapTap,
                    personName = personName,
                    personPhotoUri = personPhotoUri,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            OutsideMapSearchBar(
                                query = searchQuery,
                                results = searchResults,
                                loading = searchLoading,
                                error = searchError,
                                onQueryChange = onSearchQueryChange,
                                onSearch = onSearch,
                                onSelectResult = onSelectSearchResult,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutsideMapControlButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close fullscreen map",
                                onClick = onDismiss
                            )
                        }
                        if (historyMode) {
                            Text(
                                "History map · choose the period to display",
                                style = MaterialTheme.typography.labelSmall,
                                color = OneCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                historyRanges.forEach { range ->
                                    AssistChip(
                                        onClick = { onHistoryRangeSelected(range.id) },
                                        label = { Text(range.label) },
                                        leadingIcon = if (range.id == selectedHistoryRangeId) {
                                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                OutsideMapControls(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onRecenter = onRecenter
                )
                if (historyMode) {
                    OutsideHistoryDetailsPanel(
                        points = historyPoints,
                        home = home,
                        safePlaces = safePlaces,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 76.dp, bottom = 16.dp)
                    )
                }
                Text(
                    "© OpenStreetMap · OpenFreeMap",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .widthIn(max = 220.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

private fun outsideHistoryPointsForMap(points: List<OneOutsideLocationPoint>): List<OneExteriorPoint> {
    val maxMapPoints = 1_500
    if (points.size <= maxMapPoints) return points.map { it.point }
    return (0 until maxMapPoints).map { index ->
        points[(index.toLong() * points.lastIndex / (maxMapPoints - 1)).toInt()].point
    }
}

@Composable
private fun OutsideHistoryDetailsPanel(
    points: List<OneOutsideLocationPoint>,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 230.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Location details", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "${points.size} points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val displayedPoints = points.asReversed().take(8)
            if (displayedPoints.isEmpty()) {
                Text(
                    "No location in this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                displayedPoints.forEachIndexed { index, point ->
                    OutsideHistoryRow(
                        point = point,
                        home = home,
                        safePlaces = safePlaces,
                        detailed = true
                    )
                    if (index < displayedPoints.lastIndex) HorizontalDivider()
                }
                if (points.size > displayedPoints.size) {
                    Text(
                        "Showing the latest ${displayedPoints.size} points",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OutsideMapSearchBar(
    query: String,
    results: List<OneOutsideLocationSearchResult>,
    loading: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectResult: (OneOutsideLocationSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it.take(160)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search location") },
            placeholder = { Text("Address, place or city") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onSearch, enabled = query.isNotBlank()) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            },
            singleLine = true
        )
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (results.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "Search results",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OneCyan,
                        fontWeight = FontWeight.Bold
                    )
                    results.take(5).forEachIndexed { index, result ->
                        TextButton(
                            onClick = { onSelectResult(result) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    result.displayName,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Tap to center the map",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < results.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
        Text(
            "Search uses the configured ONE location service",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OutsideMapControls(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutsideMapControlButton(Icons.Default.ZoomIn, "Zoom in", onZoomIn)
        OutsideMapControlButton(Icons.Default.ZoomOut, "Zoom out", onZoomOut)
        OutsideMapControlButton(Icons.Default.MyLocation, "Center on latest location", onRecenter)
    }
}

@Composable
private fun OutsideMapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 5.dp,
        shadowElevation = 5.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = OneBlue)
        }
    }
}

@Composable
private fun OutsideHeader(kicker: String, title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(kicker, style = MaterialTheme.typography.labelSmall, color = OneCyan, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OutsideInfoCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OutsideNotice(
    title: String,
    detail: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.09f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                action()
            }
        }
    }
}

@Composable
private fun BoxWithContent(expanded: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text("Change") },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun OutsidePlaceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    detail: String,
    onEdit: (() -> Unit)?,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = OneBlue, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        onEdit?.let { TextButton(onClick = it) { Text("Edit") } }
        TextButton(onClick = onMove) { Text("Move") }
        TextButton(onClick = onDelete) { Text("Remove") }
    }
}

@Composable
private fun OutsideStatusCard(
    zone: OneExteriorZoneMatch?,
    lastPoint: OneOutsideLocationPoint?,
    homeConfigured: Boolean,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Current situation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (zone?.kind == OneExteriorZoneKind.OUTSIDE) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (zone?.kind == OneExteriorZoneKind.OUTSIDE) OneAmber else OneMint
                )
            }
            Text(
                lastPoint?.let { oneOutsideLocationLabel(it, home, safePlaces) }
                    ?: zone?.label
                    ?: if (homeConfigured) "Waiting for a reliable location" else "Set home to classify locations",
                style = MaterialTheme.typography.titleSmall
            )
            lastPoint?.let { point ->
                Text(
                    "Last point · " + formatOutsideTime(point.capturedAtMillis) +
                        " · " + (point.source.wireValue.uppercase(Locale.US)) +
                        (point.accuracyMeters?.let { " · ±" + it.toInt() + " m" } ?: "") +
                        (point.dwellDurationMillis.takeIf { it >= ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS }
                            ?.let { " · stayed " + formatOutsideDuration(it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OutsideHistoryRow(
    point: OneOutsideLocationPoint,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    detailed: Boolean = false
) {
    val label = oneOutsideLocationLabel(point, home, safePlaces)
    val pointDetails = listOfNotNull(
        formatOutsideTime(point.capturedAtMillis),
        point.source.wireValue.uppercase(Locale.US),
        point.accuracyMeters?.let { "±${it.toInt()} m" },
        point.dwellDurationMillis.takeIf { it >= ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS }
            ?.let { "stayed ${formatOutsideDuration(it)}" }
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Location " + label + " at " + formatOutsideTime(point.capturedAtMillis) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (point.source == OneOutsideLocationSource.GPS) Icons.Default.MyLocation else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (point.source == OneOutsideLocationSource.GPS) OneBlue else OneCyan,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                if (detailed) pointDetails else formatOutsideTime(point.capturedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (detailed) {
                Text(
                    String.format(Locale.US, "%.5f, %.5f", point.point.latitude, point.point.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatOutsideTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.ENGLISH))

private fun Double.toDisplayRadius(): String = String.format(Locale.US, "%.0f", this)

private fun formatOutsideDuration(durationMillis: Long): String {
    val totalMinutes = (durationMillis / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun outsideInitials(name: String): String = name
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "?" }

internal fun hasOutsideLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun hasOutsideBackgroundLocationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun isSystemLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) manager.isLocationEnabled
    else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun readOutsideBatteryPercent(context: Context): Int? {
    val batteryManager = context.getSystemService(BatteryManager::class.java) ?: return null
    return batteryManager
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        .takeIf { it in 0..100 }
}

@Composable
private fun OutsideDialog(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: () -> Unit,
    confirmLabel: String,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = content,
        confirmButton = {
            Button(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
