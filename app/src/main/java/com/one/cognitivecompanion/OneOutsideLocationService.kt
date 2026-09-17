package com.one.cognitivecompanion

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class OneOutsideLocationService : Service() {
    private lateinit var store: OneOutsideTrackingStore
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var activePersonId: UUID? = null
    private var lastRecordedCapturedAtMillis = 0L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reverseLookupAttempts = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        store = OneOutsideTrackingStore(this)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        OneOutsideNotificationHelper.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requestedPerson = intent?.getStringExtra(EXTRA_PERSON_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        activePersonId = requestedPerson ?: activePersonId ?: store.readSelectedPersonId()
        val personId = activePersonId
        if (personId == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_STOP) {
            store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.GPS)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == null && !store.read(personId).trackingEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground(personId)
        if (!hasLocationPermission()) {
            store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.GPS)
            updateNotification("Allow location access to continue tracking")
            sendTrackingBroadcast(personId, error = "Location permission is no longer available.")
            stopSelf()
            return START_NOT_STICKY
        }
        requestLocationUpdates(personId)
        requestLastKnownLocation(personId)
        if (intent?.action == ACTION_SYNC_GEOFENCES || intent?.action == null) {
            registerGeofences(personId)
        }
        return START_STICKY
    }

    private fun startAsForeground(personId: UUID) {
        val notification = OneOutsideNotificationHelper.trackingNotification(
            this,
            "Outside tracking active",
            "Updating the location for ${personId.shortLabel()}"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                OneOutsideNotificationHelper.TRACKING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(OneOutsideNotificationHelper.TRACKING_NOTIFICATION_ID, notification)
        }
    }

    private fun requestLocationUpdates(personId: UUID) {
        if (locationCallback != null) return
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            ONE_OUTSIDE_LOCATION_INTERVAL_MILLIS
        )
            .setMinUpdateIntervalMillis(ONE_OUTSIDE_MIN_LOCATION_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(ONE_OUTSIDE_MIN_LOCATION_DISTANCE_METERS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                recordGpsLocation(personId, location)
            }
        }
        locationCallback = callback
        try {
            locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    updateNotification(error.message ?: "Location updates unavailable")
                    sendTrackingBroadcast(personId, error = error.message ?: "Location updates unavailable.")
                }
        } catch (error: SecurityException) {
            store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.GPS)
            updateNotification("Allow location access to continue tracking")
            sendTrackingBroadcast(personId, error = error.message ?: "Location permission is required.")
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLastKnownLocation(personId: UUID) {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return
        runCatching {
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) recordGpsLocation(personId, location)
                }
        }
    }

    private fun recordGpsLocation(personId: UUID, location: android.location.Location) {
        val capturedAtMillis = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        if (capturedAtMillis <= lastRecordedCapturedAtMillis) return
        val update = runCatching {
            store.appendLocation(
                personId = personId,
                point = OneExteriorPoint(location.latitude, location.longitude),
                accuracyMeters = location.accuracy.takeIf { it >= 0f },
                capturedAtMillis = capturedAtMillis,
                source = OneOutsideLocationSource.GPS
            )
        }.getOrNull() ?: return
        lastRecordedCapturedAtMillis = capturedAtMillis
        update.alerts.forEach { alert ->
            OneOutsideNotificationHelper.notifyAlert(this, alert)
        }
        updateNotification(
            if (update.classified) {
                "Last update: ${update.zone.label}"
            } else {
                "Last update saved · accuracy too low for zone alerts"
            }
        )
        sendTrackingBroadcast(personId)
        scheduleStreetLookupIfNeeded(update)
    }

    private fun scheduleStreetLookupIfNeeded(update: OneOutsideLocationResult) {
        val point = update.point
        if (point.source != OneOutsideLocationSource.GPS ||
            point.streetName != null ||
            (point.zoneKey != "outside" && point.dwellDurationMillis < ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS)
        ) {
            return
        }
        val key = String.format(
            Locale.US,
            "%.4f:%.4f",
            point.point.latitude,
            point.point.longitude
        )
        synchronized(reverseLookupAttempts) {
            val lastAttempt = reverseLookupAttempts[key]
            if (lastAttempt != null &&
                System.currentTimeMillis() - lastAttempt < STREET_LOOKUP_RETRY_INTERVAL_MILLIS
            ) {
                return
            }
            reverseLookupAttempts[key] = System.currentTimeMillis()
        }
        serviceScope.launch {
            val streetName = runCatching { reverseGeocodeOneOutsideLocation(point.point) }.getOrNull()
            if (!streetName.isNullOrBlank()) {
                store.updateLocationStreetName(
                    personId = point.personId,
                    pointId = point.id,
                    point = point.point,
                    capturedAtMillis = point.capturedAtMillis,
                    dwellDurationMillis = point.dwellDurationMillis,
                    streetName = streetName
                )
                sendTrackingBroadcast(point.personId)
            }
        }
    }

    private fun registerGeofences(personId: UUID) {
        val snapshot = store.read(personId)
        val geofences = buildList {
            snapshot.home?.let { home ->
                add(
                    buildGeofence(
                        personId = personId,
                        placeId = HOME_PLACE_ID,
                        center = home.center,
                        radiusMeters = home.radiusMeters
                    )
                )
            }
            snapshot.safePlaces.take(99).forEach { place ->
                add(
                    buildGeofence(
                        personId = personId,
                        placeId = place.id,
                        center = place.center,
                        radiusMeters = place.radiusMeters
                    )
                )
            }
        }
        val client = LocationServices.getGeofencingClient(this)
        runCatching {
            client.removeGeofences(geofencePendingIntent())
                .addOnCompleteListener {
                    if (geofences.isEmpty() || !hasLocationPermission()) return@addOnCompleteListener
                    val request = GeofencingRequest.Builder()
                        .setInitialTrigger(0)
                        .addGeofences(geofences)
                        .build()
                    try {
                        client.addGeofences(request, geofencePendingIntent())
                            .addOnFailureListener { error ->
                                sendTrackingBroadcast(
                                    personId,
                                    error = error.message ?: "Could not register location zones."
                                )
                            }
                    } catch (_: SecurityException) {
                        sendTrackingBroadcast(personId, error = "Background location permission is required for zone alerts.")
                    }
                }
        }.onFailure {
            sendTrackingBroadcast(personId, error = it.message ?: "Could not register location zones.")
        }
    }

    private fun buildGeofence(
        personId: UUID,
        placeId: String,
        center: OneExteriorPoint,
        radiusMeters: Double
    ): Geofence = Geofence.Builder()
        .setRequestId(geofenceId(personId, placeId))
        .setCircularRegion(
            center.latitude,
            center.longitude,
            radiusMeters.coerceAtLeast(ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS).toFloat()
        )
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setNotificationResponsiveness(GEOFENCE_RESPONSIVENESS_MILLIS)
        .setTransitionTypes(
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        .build()

    private fun geofencePendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            GEOFENCE_REQUEST_CODE,
            Intent(this, OneOutsideGeofenceReceiver::class.java).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun updateNotification(text: String) {
        OneOutsideNotificationHelper.updateTrackingNotification(this, text)
    }

    private fun sendTrackingBroadcast(personId: UUID, error: String? = null) {
        sendBroadcast(
            Intent(ACTION_TRACKING_CHANGED)
                .setPackage(packageName)
                .putExtra(EXTRA_PERSON_ID, personId.toString())
                .putExtra(EXTRA_ERROR, error)
        )
    }

    override fun onDestroy() {
        locationCallback?.let { callback ->
            locationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
        runCatching { LocationServices.getGeofencingClient(this).removeGeofences(geofencePendingIntent()) }
        activePersonId?.let { sendTrackingBroadcast(it) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun UUID.shortLabel(): String = toString().take(8)

    companion object {
        const val ACTION_SYNC_GEOFENCES = "com.one.cognitivecompanion.action.SYNC_OUTSIDE_GEOFENCES"
        const val ACTION_STOP = "com.one.cognitivecompanion.action.STOP_OUTSIDE_TRACKING"
        const val ACTION_TRACKING_CHANGED = "com.one.cognitivecompanion.action.OUTSIDE_TRACKING_CHANGED"
        const val EXTRA_PERSON_ID = "outside_person_id"
        const val EXTRA_ERROR = "outside_tracking_error"
        const val HOME_PLACE_ID = "home"
        private const val GEOFENCE_REQUEST_CODE = 9_431
        private const val GEOFENCE_RESPONSIVENESS_MILLIS = 300_000
        private const val STREET_LOOKUP_RETRY_INTERVAL_MILLIS = 300_000L

        fun start(context: Context, personId: UUID) {
            val intent = Intent(context, OneOutsideLocationService::class.java)
                .putExtra(EXTRA_PERSON_ID, personId.toString())
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val store = OneOutsideTrackingStore(context)
            store.readSelectedPersonId()?.let { personId ->
                store.setTrackingEnabled(personId, false, OneOutsideTrackingMode.GPS)
            }
            context.stopService(Intent(context, OneOutsideLocationService::class.java).setAction(ACTION_STOP))
        }

        fun syncGeofences(context: Context, personId: UUID) {
            val intent = Intent(context, OneOutsideLocationService::class.java)
                .setAction(ACTION_SYNC_GEOFENCES)
                .putExtra(EXTRA_PERSON_ID, personId.toString())
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

class OneOutsideGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val store = OneOutsideTrackingStore(context)
        event.triggeringGeofences.orEmpty().forEach { geofence ->
            val parsed = parseGeofenceId(geofence.requestId) ?: return@forEach
            val alert = store.recordGeofenceSignal(
                personId = parsed.first,
                placeId = parsed.second,
                transition = event.geofenceTransition
            )
            if (alert != null) {
                OneOutsideNotificationHelper.notifyAlert(context, alert)
                context.sendBroadcast(
                    Intent(OneOutsideLocationService.ACTION_TRACKING_CHANGED)
                        .setPackage(context.packageName)
                        .putExtra(OneOutsideLocationService.EXTRA_PERSON_ID, parsed.first.toString())
                )
            }
        }
    }

    private fun parseGeofenceId(value: String?): Pair<UUID, String>? {
        val parts = value?.split('|', limit = 3) ?: return null
        if (parts.size != 3 || parts[0] != GEOFENCE_PREFIX) return null
        return runCatching { UUID.fromString(parts[1]) to parts[2] }.getOrNull()
    }

    private companion object {
        const val GEOFENCE_PREFIX = "one-outside"
    }
}

private fun geofenceId(personId: UUID, placeId: String): String =
    "one-outside|$personId|$placeId"

internal object OneOutsideNotificationHelper {
    const val TRACKING_NOTIFICATION_ID = 1_210
    private const val ALERT_NOTIFICATION_ID = 1_211
    private const val RESIDENT_MESSAGE_NOTIFICATION_ID = 1_212
    private const val TRACKING_CHANNEL_ID = "one_outside_tracking"
    private const val ALERT_CHANNEL_ID = "one_outside_alerts"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Outside tracking",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "Outside companion alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    fun trackingNotification(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, TRACKING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun updateTrackingNotification(context: Context, text: String) {
        ensureChannels(context)
        context.getSystemService(NotificationManager::class.java)
            ?.notify(TRACKING_NOTIFICATION_ID, trackingNotification(context, "Outside tracking active", text))
    }

    fun notifyAlert(context: Context, alert: OneOutsideAlert) {
        ensureChannels(context)
        context.getSystemService(NotificationManager::class.java)?.notify(
            ALERT_NOTIFICATION_ID,
            NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(alert.title)
                .setContentText(alert.detail)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.detail))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    fun notifyResidentMessage(context: Context, personName: String, message: String) {
        ensureChannels(context)
        context.getSystemService(NotificationManager::class.java)?.notify(
            RESIDENT_MESSAGE_NOTIFICATION_ID,
            NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("ONE · Message for $personName")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }
}
