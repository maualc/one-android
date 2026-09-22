package com.one.cognitivecompanion

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

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

    suspend fun synchronize(careRecipientId: UUID): OneLocationSyncStatus = withContext(Dispatchers.IO) {
        val session = secureStore.restore()?.session
            ?: return@withContext status(careRecipientId, "Sign in to synchronize location.")
        try {
            val deviceId = deviceId(careRecipientId)
            apiClient.registerTrackingDevice(
                session,
                careRecipientId,
                deviceId,
                "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(120).ifBlank { "Android phone" }
            )
            val battery = (appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
            val cutoff = Instant.now().minusSeconds(7 * 86_400L)
            store.read(careRecipientId).points
                .asSequence()
                .filter { it.source != OneOutsideLocationSource.REMOTE }
                .filter { Instant.ofEpochMilli(it.capturedAtMillis).isAfter(cutoff) }
                .chunked(200)
                .forEach { points ->
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
                                capturedAt = Instant.ofEpochMilli(point.capturedAtMillis)
                            )
                        }
                    )
                }
            synchronizeSafePlaces(session, careRecipientId)
            pullRemote(session, careRecipientId)
            val now = System.currentTimeMillis()
            preferences.edit()
                .putLong(lastSyncKey(careRecipientId), now)
                .remove(errorKey(careRecipientId))
                .apply()
            OneLocationSyncStatus(now, null)
        } catch (error: Exception) {
            status(careRecipientId, error.message ?: "Location synchronization failed.")
        }
    }

    suspend fun pull(careRecipientId: UUID): OneLocationSyncStatus = withContext(Dispatchers.IO) {
        val session = secureStore.restore()?.session
            ?: return@withContext status(careRecipientId, "Sign in to load shared location.")
        try {
            pullRemote(session, careRecipientId)
            val now = System.currentTimeMillis()
            preferences.edit().putLong(lastSyncKey(careRecipientId), now).remove(errorKey(careRecipientId)).apply()
            OneLocationSyncStatus(now, null)
        } catch (error: Exception) {
            status(careRecipientId, error.message ?: "Shared location is unavailable.")
        }
    }

    suspend fun saveSafePlace(careRecipientId: UUID, place: OneExteriorSafePlace): OneLocationSyncStatus =
        withContext(Dispatchers.IO) {
            store.saveSafePlace(careRecipientId, place)
            val session = secureStore.restore()?.session
                ?: return@withContext status(careRecipientId, "Sign in to synchronize safe places.")
            try {
                upsertSafePlace(session, careRecipientId, place, apiClient.safePlaces(session, careRecipientId))
                pullRemoteSafePlaces(session, careRecipientId)
                success(careRecipientId)
            } catch (error: Exception) {
                status(careRecipientId, error.message ?: "The safe place could not be synchronized.")
            }
        }

    suspend fun deleteSafePlace(careRecipientId: UUID, place: OneExteriorSafePlace): OneLocationSyncStatus =
        withContext(Dispatchers.IO) {
            store.removeSafePlace(careRecipientId, place.id)
            val session = secureStore.restore()?.session
                ?: return@withContext status(careRecipientId, "Sign in to synchronize safe places.")
            try {
                val remote = findRemotePlace(place, apiClient.safePlaces(session, careRecipientId))
                if (remote != null) apiClient.deleteSafePlace(session, careRecipientId, remote.id)
                success(careRecipientId)
            } catch (error: Exception) {
                status(careRecipientId, error.message ?: "The safe place could not be removed from ONE.")
            }
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

    private suspend fun pullRemote(session: OneSession, careRecipientId: UUID) {
        val since = Instant.now().minusSeconds(7 * 86_400L)
        apiClient.locationHistory(session, careRecipientId, since).forEach { point ->
            store.upsertRemoteLocation(
                personId = careRecipientId,
                remoteId = point.id,
                point = OneExteriorPoint(point.latitude, point.longitude),
                accuracyMeters = point.accuracyM?.toFloat(),
                capturedAtMillis = point.capturedAt.toEpochMilli()
            )
        }
        pullRemoteSafePlaces(session, careRecipientId)
    }

    private suspend fun synchronizeSafePlaces(session: OneSession, careRecipientId: UUID) {
        val remotePlaces = apiClient.safePlaces(session, careRecipientId)
        store.read(careRecipientId).safePlaces.forEach { local ->
            upsertSafePlace(session, careRecipientId, local, remotePlaces)
        }
    }

    private suspend fun upsertSafePlace(
        session: OneSession,
        careRecipientId: UUID,
        local: OneExteriorSafePlace,
        remotePlaces: List<OneRemoteSafePlace>
    ) {
        val remote = findRemotePlace(local, remotePlaces)
        val saved = if (remote == null) {
            apiClient.createSafePlace(session, careRecipientId, local)
        } else {
            val changed = !remote.name.equals(local.name, ignoreCase = false) ||
                OneExteriorPoint(remote.latitude, remote.longitude).distanceTo(local.center) > 1.0 ||
                kotlin.math.abs(remote.radiusM - local.radiusMeters) > 1.0
            if (changed) apiClient.updateSafePlace(session, careRecipientId, remote.id, local) else remote
        }
        if (saved.id.toString() != local.id) {
            store.removeSafePlace(careRecipientId, local.id)
            store.saveSafePlace(careRecipientId, saved.toLocal())
        }
    }

    private fun findRemotePlace(local: OneExteriorSafePlace, remotePlaces: List<OneRemoteSafePlace>): OneRemoteSafePlace? {
        val localUuid = runCatching { UUID.fromString(local.id) }.getOrNull()
        return remotePlaces.firstOrNull { it.id == localUuid }
            ?: remotePlaces.firstOrNull { remote ->
                remote.name.equals(local.name, ignoreCase = true) &&
                    OneExteriorPoint(remote.latitude, remote.longitude).distanceTo(local.center) <= 25.0
            }
    }

    private suspend fun pullRemoteSafePlaces(session: OneSession, careRecipientId: UUID) {
        apiClient.safePlaces(session, careRecipientId).forEach { place ->
            store.saveSafePlace(
                careRecipientId,
                place.toLocal()
            )
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
        preferences.edit().putLong(lastSyncKey(careRecipientId), now).remove(errorKey(careRecipientId)).apply()
        return OneLocationSyncStatus(now, null)
    }

    private fun status(careRecipientId: UUID, error: String): OneLocationSyncStatus {
        preferences.edit().putString(errorKey(careRecipientId), error).apply()
        return readStatus(careRecipientId).copy(error = error)
    }

    private fun deviceId(careRecipientId: UUID): UUID {
        val key = deviceKey(careRecipientId)
        preferences.getString(key, null)?.let { raw ->
            runCatching { UUID.fromString(raw) }.getOrNull()?.let { return it }
        }
        return UUID.randomUUID().also { preferences.edit().putString(key, it.toString()).apply() }
    }

    private fun deviceKey(id: UUID) = "device:$id"
    private fun lastSyncKey(id: UUID) = "last_sync:$id"
    private fun errorKey(id: UUID) = "error:$id"

    private companion object {
        const val PREFERENCES_NAME = "one.outside.sync"
    }
}
