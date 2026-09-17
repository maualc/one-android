package com.one.cognitivecompanion

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private enum class OutsideMapEditMode {
    NONE,
    SET_HOME,
    ADD_SAFE_PLACE,
    MOVE_SAFE_PLACE
}

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
    isBackend: Boolean,
    careRecipientsLoading: Boolean,
    onOpenFamily: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { OneOutsideTrackingStore(context) }
    val availableRecipients = if (!isBackend && careRecipients.isNullOrEmpty()) {
        listOf(outsideDemoRecipient)
    } else {
        careRecipients.orEmpty()
    }
    var selectedPersonId by remember { mutableStateOf(store.readSelectedPersonId()) }
    var snapshot by remember { mutableStateOf<OneOutsideTrackingSnapshot?>(null) }
    var serviceError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTrackingPersonId by remember { mutableStateOf<UUID?>(null) }
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
    var messageDraft by rememberSaveable { mutableStateOf("") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showFullScreenMap by rememberSaveable { mutableStateOf(false) }
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
    val recipientIds = availableRecipients.map { it.id }
    val selectedRecipient = availableRecipients.firstOrNull { it.id == selectedPersonId }
    val selectedName = selectedRecipient?.displayName ?: "Person cared for"

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
            val results = searchOneOutsideLocations(query)
            mapSearchResults = results
            if (results.isEmpty()) {
                mapSearchError = "No se han encontrado resultados para esa búsqueda."
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            mapSearchResults = emptyList()
            mapSearchError = "No se ha podido buscar la ubicación. Comprueba la conexión e inténtalo de nuevo."
        } finally {
            mapSearchLoading = false
        }
    }

    fun reload() {
        selectedPersonId?.let { snapshot = store.read(it) }
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
        routeIndex = 0
        store.setTrackingEnabled(personId, true, OneOutsideTrackingMode.GPS)
        store.saveSelectedPersonId(personId)
        OneOutsideLocationService.start(context, personId)
        serviceError = null
        reload()
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
            store.setTrackingEnabled(personId, true, OneOutsideTrackingMode.GPS)
            store.saveSelectedPersonId(personId)
            OneOutsideLocationService.start(context, personId)
            serviceError = null
            reload()
        } else {
            serviceError = "Location permission is required to record this phone's position."
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) serviceError = "Notifications are disabled; in-app alerts will still be kept in the local history."
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
        serviceError = null
        reload()
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = ("package:" + context.packageName).toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    LaunchedEffect(recipientIds) {
        val current = store.readSelectedPersonId()
        val valid = current?.takeIf { it in recipientIds } ?: recipientIds.firstOrNull()
        if (valid != current) store.saveSelectedPersonId(valid)
        selectedPersonId = valid
    }

    LaunchedEffect(selectedPersonId) {
        simulationRunning = false
        routeIndex = 0
        snapshot = selectedPersonId?.let { store.read(it) }
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

    val routePoints = snapshot?.home?.let { home ->
        oneOutsideRoutePoints(routeId, home, snapshot?.safePlaces.orEmpty())
    }.orEmpty()
    val currentPoint = snapshot?.lastPoint?.point
    val plannedPoints = if (simulationRunning) routePoints else emptyList()
    val mapPoints = snapshot?.points.orEmpty().map { it.point }.takeLast(150) + plannedPoints
    val canAdvance = simulationRunning && routePoints.isNotEmpty() && routeIndex < routePoints.lastIndex
    val currentZone = snapshot?.currentZone
    val locationEnabled = isSystemLocationEnabled(context)
    val hasBackgroundPermission = hasOutsideBackgroundLocationPermission(context)
    val latestReliablePoint = snapshot?.points.orEmpty().lastOrNull { it.zoneKey != null }
    val history = snapshot?.points.orEmpty().asReversed()

    fun recenterMap() {
        val target = latestReliablePoint?.point ?: currentPoint ?: snapshot?.home?.center
        if (target == null) {
            mapSearchError = "Todavía no hay una ubicación disponible para recentrar el mapa."
            return
        }
        mapSearchPoint = null
        mapSearchResults = emptyList()
        mapSearchError = null
        issueMapCameraCommand(OneExteriorMapCameraAction.RECENTER, target)
    }

    fun openFullScreenMap() {
        // A new MapView is created for the dialog. The current focus point is
        // enough to restore the useful camera target without replaying an old
        // zoom command on the new instance.
        mapCameraCommand = null
        showFullScreenMap = true
    }

    fun closeFullScreenMap() {
        mapCameraCommand = null
        showFullScreenMap = false
    }

    fun handleMapTap(point: OneExteriorPoint) {
        val personId = selectedPersonId ?: return
        when (mapEditMode) {
            OutsideMapEditMode.SET_HOME -> {
                val radius = snapshot?.home?.radiusMeters ?: ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS
                store.saveHome(personId, point, radius)
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
        if (snapshot?.home == null) return
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

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                        Icon(Icons.Default.Person, contentDescription = null, tint = OneBlue, modifier = Modifier.size(22.dp))
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
                        if (isBackend) "Only this Android phone is linked in this prototype. Nothing is sent to the backend."
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
                                    else -> "Paused · use a simulated route to test the map and alerts."
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
                            "Last reliable point · " + formatOutsideTime(latestReliablePoint.capturedAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!locationEnabled) {
                        OutsideNotice(
                            title = "Location services are off",
                            detail = "Turn on Android location services before starting a real local trace.",
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
                            title = "Allow background location for zone alerts",
                            detail = "Android can keep the visible tracking service running, but geofence alerts work best after choosing “Allow all the time” in app settings.",
                            tint = OneAmber,
                            icon = Icons.Default.LocationOn,
                            action = { OutlinedButton(onClick = ::openAppSettings) { Text("Open app settings") } }
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
        if (!showFullScreenMap) {
            item {
                OutsideMapSection(
                    home = snapshot?.home,
                    safePlaces = snapshot?.safePlaces.orEmpty(),
                    routePoints = mapPoints,
                    currentPoint = currentPoint,
                    focusPoint = mapSearchPoint ?: currentPoint,
                    searchPoint = mapSearchPoint,
                    cameraCommand = mapCameraCommand,
                    onMapTap = ::handleMapTap,
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
                    onOpenFullScreen = ::openFullScreenMap
                )
            }
            item {
                Text(
                    "© OpenStreetMap contributors · Map data is used under the OSM Tile Usage Policy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                    detail = "The circle is the configured area. It is not a promise of exact GPS accuracy.",
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
                                mapEditModeName = OutsideMapEditMode.SET_HOME.name
                                showHomeRadiusDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text(if (snapshot?.home == null) "Set home" else "Move home")
                        }
                        if (snapshot?.home != null) {
                            OutlinedButton(
                                onClick = {
                                    homeRadius = snapshot?.home?.radiusMeters?.toDisplayRadius()
                                        ?: ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS.toDisplayRadius()
                                    showHomeRadiusDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Home radius") }
                        }
                    }
                    latestReliablePoint?.let { point ->
                        OutlinedButton(
                            onClick = {
                                selectedPersonId?.let { personId ->
                                    store.saveHome(
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
                            onEdit = null,
                            onMove = {
                                mapEditModeName = OutsideMapEditMode.SET_HOME.name
                            },
                            onDelete = {
                                selectedPersonId?.let { personId ->
                                    store.removeHome(personId)
                                    syncZonesIfTracking(personId)
                                    reload()
                                }
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            mapEditModeName = OutsideMapEditMode.ADD_SAFE_PLACE.name
                            movingSafePlaceId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Add safe place")
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
                                    mapEditModeName = OutsideMapEditMode.MOVE_SAFE_PLACE.name
                                    movingSafePlaceId = place.id
                                },
                                onDelete = {
                                    selectedPersonId?.let { personId ->
                                        store.removeSafePlace(personId, place.id)
                                        syncZonesIfTracking(personId)
                                        reload()
                                    }
                                }
                            )
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
                                        routeIndex = 0
                                        simulationRunning = false
                                        routeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (snapshot?.home == null) {
                        Text("Set home before starting a route.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(
                            (currentZone?.label ?: "Ready") + " · step " +
                                (routeIndex + 1).coerceAtMost(routePoints.size) + "/" + routePoints.size,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        item {
            OutsideStatusCard(
                zone = currentZone,
                lastPoint = snapshot?.lastPoint,
                homeConfigured = snapshot?.home != null
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
                        Text("Remembered route", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.History, contentDescription = null, tint = OneBlue)
                    }
                    Text(
                        outsideRecallSummary(snapshot, selectedName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (history.isEmpty()) {
                        Text("No points recorded in the last 7 days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        history.take(12).forEach { point ->
                            OutsideHistoryRow(point, snapshot?.home, snapshot?.safePlaces.orEmpty())
                        }
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = ::reload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Refresh")
                        }
                        OutlinedButton(
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
                "Location history is approximate. Android may delay background updates to save battery, and a zone alert does not prove an exact position.",
                style = MaterialTheme.typography.bodySmall,
                color = OneAmber
            )
        }
    }

    if (showFullScreenMap) {
        OutsideFullScreenMapDialog(
            home = snapshot?.home,
            safePlaces = snapshot?.safePlaces.orEmpty(),
            routePoints = mapPoints,
            currentPoint = currentPoint,
            focusPoint = mapSearchPoint ?: currentPoint,
            searchPoint = mapSearchPoint,
            cameraCommand = mapCameraCommand,
            onMapTap = ::handleMapTap,
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
                        label = { Text("Radius in metres (100 minimum)") },
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
                    store.saveHome(personId, home.center, radius)
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
                    label = { Text("Radius in metres (100 minimum)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
    if (showClearHistoryDialog) {
        OutsideDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = "Clear local history?",
            onConfirm = {
                selectedPersonId?.let { store.clearHistory(it) }
                showClearHistoryDialog = false
                reload()
            },
            confirmLabel = "Clear",
            content = {
                Text("This removes the last 7 days of points and outside alerts for " + selectedName + " from this phone.")
            }
        )
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
                    currentPoint = currentPoint,
                    focusPoint = focusPoint,
                    searchPoint = searchPoint,
                    cameraCommand = cameraCommand,
                    onMapTap = onMapTap,
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
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
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
                Text(
                    "© OpenStreetMap contributors",
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
            "Search powered by OpenStreetMap / Nominatim",
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
    onClick: () -> Unit
) {
    Surface(
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
    homeConfigured: Boolean
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
                zone?.label ?: if (homeConfigured) "Waiting for a reliable location" else "Set home to classify locations",
                style = MaterialTheme.typography.titleSmall
            )
            lastPoint?.let { point ->
                Text(
                    "Last point · " + formatOutsideTime(point.capturedAtMillis) +
                        " · " + (point.source.wireValue.uppercase(Locale.US)) +
                        (point.accuracyMeters?.let { " · ±" + it.toInt() + " m" } ?: ""),
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
    safePlaces: List<OneExteriorSafePlace>
) {
    val zone = point.zoneKey?.let { outsideZoneForKey(it, home, safePlaces) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Location " + (zone?.label ?: "unclassified") + " at " + formatOutsideTime(point.capturedAtMillis) },
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
            Text(zone?.label ?: "Location saved; accuracy too low to classify", style = MaterialTheme.typography.bodySmall)
            Text(formatOutsideTime(point.capturedAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        point.accuracyMeters?.let {
            Text("±" + it.toInt() + " m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun outsideRecallSummary(snapshot: OneOutsideTrackingSnapshot?, personName: String): String {
    val points = snapshot?.points.orEmpty()
    if (points.isEmpty()) return "I have no reliable location history for " + personName + " yet."
    val places = points.mapNotNull { point ->
        point.zoneKey?.let { outsideZoneForKey(it, snapshot?.home, snapshot?.safePlaces.orEmpty()).label }
    }.distinct()
    val last = points.lastOrNull { it.zoneKey != null }
    val visited = places.joinToString(", ").ifBlank { "unclassified places" }
    return "I remember " + visited + ". Last reliable record: " +
        (last?.let { formatOutsideTime(it.capturedAtMillis) } ?: "not available") + "."
}

private fun formatOutsideTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.ENGLISH))

private fun Double.toDisplayRadius(): String = String.format(Locale.US, "%.0f", this)

private fun hasOutsideLocationPermission(context: Context): Boolean =
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
