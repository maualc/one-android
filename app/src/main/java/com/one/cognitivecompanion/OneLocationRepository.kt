package com.one.cognitivecompanion

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class OneLocationSyncStatus(
    val lastSuccessfulSyncMillis: Long?,
    val error: String?
)

/**
 * Local-first bridge between the foreground location service and ONE.
 * Uploads are intentionally idempotent: every local SQLite point keeps its
 * UUID as the backend client sample id, so replaying after an outage is safe.
 */
class OneLocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = OneOutsideTrackingStore(appContext)
    private val secureStore = OneSecureStore(appContext)
    private val apiClient: OneApiClient = OneHttpApiClient(RuntimeConfiguration())
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun synchronize(careRecipientId: UUID): OneLocationSyncStatus = withContext(Dispatchers.IO) { syncMutex.withLock {
        val session = secureStore.restore()?.session
            ?: return@withContext status(careRecipientId, "Sign in to synchronize location.")
        try {
            val deviceId = deviceId(careRecipientId)
            val device = apiClient.registerTrackingDevice(
                session,
                careRecipientId,
                deviceId,
                "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(120).ifBlank { "Android phone" }
            )
            val battery = (appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
            pullState(session, careRecipientId)
            while (device.status == "active") {
                val points = store.pendingPoints(careRecipientId)
                if (points.isEmpty()) break
                apiClient.uploadLocationPoints(
                    session,
                    careRecipientId,
                    deviceId,
                    points.map { point ->
                        OneLocationPointUpload(
                            clientSampleId = point.id,
                            latitude = point.point.latitude,
                            longitude = point.point.longitude,
                            accuracyM = point.accuracyMeters?.toDouble(),
                            batteryPercent = battery,
                            capturedAt = Instant.ofEpochMilli(point.capturedAtMillis),
                            streetName = point.streetName,
                            dwellDurationMillis = point.dwellDurationMillis
                        )
                    }
                )
                store.markPointsUploaded(points)
            }
            val zoneKey = "zones_last_pull:$careRecipientId"
            if (store.pendingPlaces(careRecipientId).isNotEmpty() || !preferences.getBoolean("zones_bootstrapped:$careRecipientId", false)
                || System.currentTimeMillis() - preferences.getLong(zoneKey, 0L) >= 60_000L) {
                synchronizeSafePlaces(session, careRecipientId)
                pullRemoteSafePlaces(session, careRecipientId)
                preferences.edit { putLong(zoneKey, System.currentTimeMillis()) }
                if (store.read(careRecipientId).trackingEnabled) OneOutsideLocationService.syncGeofences(appContext, careRecipientId)
            }
            val now = System.currentTimeMillis()
            preferences.edit { putLong(lastSyncKey(careRecipientId), now); remove(errorKey(careRecipientId)) }
            OneLocationSyncStatus(now, null)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            status(careRecipientId, error.message ?: "Location synchronization failed.")
        }
    } }

    suspend fun pull(careRecipientId: UUID): OneLocationSyncStatus = withContext(Dispatchers.IO) { syncMutex.withLock {
        val session = secureStore.restore()?.session
            ?: return@withContext status(careRecipientId, "Sign in to load shared location.")
        try {
            synchronizeSafePlaces(session, careRecipientId)
            pullRemote(session, careRecipientId)
            val now = System.currentTimeMillis()
            preferences.edit { putLong(lastSyncKey(careRecipientId), now); remove(errorKey(careRecipientId)) }
            OneLocationSyncStatus(now, null)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            status(careRecipientId, error.message ?: "Shared location is unavailable.")
        }
    } }

    suspend fun saveSafePlace(careRecipientId: UUID, place: OneExteriorSafePlace): OneLocationSyncStatus =
        withContext(Dispatchers.IO) {
            store.saveSafePlace(careRecipientId, place)
            store.queuePlace(careRecipientId, place.id, "safe", "save")
            synchronize(careRecipientId)
        }

    suspend fun deleteSafePlace(careRecipientId: UUID, place: OneExteriorSafePlace): OneLocationSyncStatus =
        withContext(Dispatchers.IO) {
            store.queuePlace(careRecipientId, place.id, "safe", "delete")
            store.removeSafePlace(careRecipientId, place.id)
            synchronize(careRecipientId)
        }

    suspend fun saveHome(careRecipientId: UUID, center: OneExteriorPoint, radius: Double): OneLocationSyncStatus = withContext(Dispatchers.IO) {
        store.saveHome(careRecipientId, center, radius)
        store.queuePlace(careRecipientId, "home", "home", "save")
        synchronize(careRecipientId)
    }

    suspend fun deleteHome(careRecipientId: UUID): OneLocationSyncStatus = withContext(Dispatchers.IO) {
        store.queuePlace(careRecipientId, "home", "home", "delete")
        store.removeHome(careRecipientId)
        synchronize(careRecipientId)
    }

    suspend fun clearSharedHistory(careRecipientId: UUID): OneLocationSyncStatus = withContext(Dispatchers.IO) {
        val session = secureStore.restore()?.session ?: return@withContext status(careRecipientId, "Sign in to clear shared history.")
        try {
            val clearedAt = apiClient.clearLocationHistory(session, careRecipientId)
            store.discardPointsThrough(careRecipientId, clearedAt.toEpochMilli())
            preferences.edit { putLong(clearKey(careRecipientId), clearedAt.toEpochMilli()) }
            success(careRecipientId)
        } catch (error: Exception) { status(careRecipientId, error.message ?: "Shared history could not be cleared.") }
    }

    suspend fun setDeviceStatus(careRecipientId: UUID, active: Boolean): OneLocationSyncStatus =
        withContext(Dispatchers.IO) {
            val session = secureStore.restore()?.session
                ?: return@withContext status(careRecipientId, "Sign in to update location sharing.")
            try {
                val id = deviceId(careRecipientId)
                apiClient.registerTrackingDevice(
                    session,
                    careRecipientId,
                    id,
                    "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(120).ifBlank { "Android phone" }
                )
                apiClient.updateTrackingDevice(session, careRecipientId, id, if (active) "active" else "paused")
                success(careRecipientId)
            } catch (error: Exception) {
                status(careRecipientId, error.message ?: "Location sharing status could not be updated.")
            }
        }

    fun readStatus(careRecipientId: UUID): OneLocationSyncStatus {
        val last = preferences.getLong(lastSyncKey(careRecipientId), 0L).takeIf { it > 0L }
        return OneLocationSyncStatus(last, preferences.getString(errorKey(careRecipientId), null))
    }

    private suspend fun pullRemote(session: OneSession, careRecipientId: UUID, includePlaces: Boolean = true) {
        val until = Instant.now()
        val since = until.minusSeconds(7 * 86_400L)
        var cursor: String? = null
        do {
            val page = apiClient.locationHistory(session, careRecipientId, since, until, cursor)
            page.clearedAt?.toEpochMilli()?.let { cleared ->
                if (cleared > preferences.getLong(clearKey(careRecipientId), 0L)) {
                    store.discardPointsThrough(careRecipientId, cleared)
                    preferences.edit { putLong(clearKey(careRecipientId), cleared) }
                }
            }
            page.points.forEach { point -> store.upsertRemoteLocation(
                personId = careRecipientId,
                remoteId = point.id,
                point = OneExteriorPoint(point.latitude, point.longitude),
                accuracyMeters = point.accuracyM?.toFloat(),
                capturedAtMillis = point.capturedAt.toEpochMilli(),
                clientSampleId = point.clientSampleId,
                streetName = point.streetName,
                dwellDurationMillis = point.dwellDurationMillis
            ) }
            cursor = page.nextCursor
        } while (cursor != null)
        if (includePlaces) pullRemoteSafePlaces(session, careRecipientId)
    }

    private suspend fun pullState(session: OneSession, careRecipientId: UUID) {
        val state = apiClient.locationState(session, careRecipientId)
        state.clearedAt?.toEpochMilli()?.let { cleared ->
            if (cleared > preferences.getLong(clearKey(careRecipientId), 0L)) {
                store.discardPointsThrough(careRecipientId, cleared)
                preferences.edit { putLong(clearKey(careRecipientId), cleared) }
            }
        }
        state.latest?.let { point ->
            store.upsertRemoteLocation(careRecipientId, point.id, OneExteriorPoint(point.latitude, point.longitude),
                point.accuracyM?.toFloat(), point.capturedAt.toEpochMilli(), point.clientSampleId,
                point.streetName, point.dwellDurationMillis)
        }
    }

    private suspend fun synchronizeSafePlaces(session: OneSession, careRecipientId: UUID) {
        val remotePlaces = apiClient.safePlaces(session, careRecipientId)
        if (!preferences.getBoolean("zones_bootstrapped:$careRecipientId", false)) {
            val local = store.read(careRecipientId)
            if (local.home != null && remotePlaces.none { it.kind == "home" } && store.pendingPlaces(careRecipientId).none { it.id == "home" })
                store.queuePlace(careRecipientId, "home", "home", "save")
            if (remotePlaces.isEmpty()) local.safePlaces.forEach { place ->
                if (store.pendingPlaces(careRecipientId).none { it.id == place.id })
                    store.queuePlace(careRecipientId, place.id, "safe", "save")
            }
        }
        for (pending in store.pendingPlaces(careRecipientId)) {
            val id = if (pending.kind == "home") remotePlaces.firstOrNull { it.kind == "home" }?.id ?: homeId(careRecipientId)
                else UUID.fromString(pending.id)
            val remote = remotePlaces.firstOrNull { it.id == id }
            if (pending.operation == "delete") {
                if (remote != null) apiClient.deleteSafePlace(session, careRecipientId, id, pending.revision)
                store.markPlaceSynced(careRecipientId, pending.id, 0)
                continue
            }
            val local = store.read(careRecipientId)
            val place = if (pending.kind == "home") local.home?.let { OneExteriorSafePlace(id.toString(), "Home", it.center, it.radiusMeters) }
                else local.safePlaces.firstOrNull { it.id == pending.id }
            if (place == null) continue
            val saved = if (remote == null) apiClient.createSafePlace(session, careRecipientId, place, pending.kind)
                else apiClient.updateSafePlace(session, careRecipientId, id, place, pending.revision)
            store.markPlaceSynced(careRecipientId, pending.id, saved.revision)
        }
        preferences.edit { putBoolean("zones_bootstrapped:$careRecipientId", true) }
    }

    private suspend fun pullRemoteSafePlaces(session: OneSession, careRecipientId: UUID) {
        val remote = apiClient.safePlaces(session, careRecipientId)
        val pending = store.pendingPlaces(careRecipientId).map { it.id }.toSet()
        val remoteIds = remote.map { if (it.kind == "home") "home" else it.id.toString() }.toSet()
        val local = store.read(careRecipientId)
        if (local.home != null && "home" !in remoteIds && "home" !in pending) store.removeHome(careRecipientId)
        local.safePlaces.filter { it.id !in remoteIds && it.id !in pending }.forEach { store.removeRemotePlace(careRecipientId, it.id) }
        remote.forEach { place ->
            val id = if (place.kind == "home") "home" else place.id.toString()
            if (id in pending) return@forEach
            if (place.kind == "home") store.saveHome(careRecipientId, OneExteriorPoint(place.latitude, place.longitude), place.radiusM)
            else store.saveSafePlace(careRecipientId, place.toLocal())
            store.markPlaceSynced(careRecipientId, id, place.revision)
        }
    }

    private fun OneRemoteSafePlace.toLocal() = OneExteriorSafePlace(
        id = id.toString(),
        name = name,
        center = OneExteriorPoint(latitude, longitude),
        radiusMeters = radiusM
    )

    private fun success(careRecipientId: UUID): OneLocationSyncStatus {
        val now = System.currentTimeMillis()
        preferences.edit { putLong(lastSyncKey(careRecipientId), now); remove(errorKey(careRecipientId)) }
        return OneLocationSyncStatus(now, null)
    }

    private fun status(careRecipientId: UUID, error: String): OneLocationSyncStatus {
        preferences.edit { putString(errorKey(careRecipientId), error) }
        return readStatus(careRecipientId).copy(error = error)
    }

    private fun deviceId(careRecipientId: UUID): UUID {
        val key = deviceKey(careRecipientId)
        preferences.getString(key, null)?.let { raw ->
            runCatching { UUID.fromString(raw) }.getOrNull()?.let { return it }
        }
        return UUID.randomUUID().also { id -> preferences.edit { putString(key, id.toString()) } }
    }

    private fun deviceKey(id: UUID) = "device:$id"
    private fun clearKey(id: UUID) = "cleared:$id"
    private fun homeId(id: UUID) = UUID.nameUUIDFromBytes("one-home:$id".toByteArray(Charsets.UTF_8))
    private fun lastSyncKey(id: UUID) = "last_sync:$id"
    private fun errorKey(id: UUID) = "error:$id"

    private companion object {
        const val PREFERENCES_NAME = "one.outside.sync"
        val syncMutex = Mutex()
    }
}
