package com.one.cognitivecompanion

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.util.UUID

/**
 * Local database for the outside companion prototype.
 *
 * This database is deliberately separate from the backend cache. It contains
 * only the local person's zones, location samples, alerts and test message.
 */
class OneOutsideTrackingStore(context: Context) {
    private val helper = OutsideTrackingDatabase(context.applicationContext)

    @Synchronized
    fun read(personId: UUID): OneOutsideTrackingSnapshot {
        val db = helper.readableDatabase
        ensureProfile(db, personId)
        return readSnapshot(db, personId)
    }

    @Synchronized
    fun readSelectedPersonId(): UUID? {
        val cursor = helper.readableDatabase.query(
            TABLE_METADATA,
            arrayOf("value"),
            "key = ?",
            arrayOf(METADATA_SELECTED_PERSON),
            null,
            null,
            null,
            "1"
        )
        return cursor.use {
            if (!it.moveToFirst()) null
            else runCatching { UUID.fromString(it.getString(0)) }.getOrNull()
        }
    }

    @Synchronized
    fun saveSelectedPersonId(personId: UUID?) {
        val db = helper.writableDatabase
        if (personId == null) {
            db.delete(TABLE_METADATA, "key = ?", arrayOf(METADATA_SELECTED_PERSON))
        } else {
            db.insertWithOnConflict(
                TABLE_METADATA,
                null,
                ContentValues().apply {
                    put("key", METADATA_SELECTED_PERSON)
                    put("value", personId.toString())
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    @Synchronized
    fun setTrackingEnabled(
        personId: UUID,
        enabled: Boolean,
        mode: OneOutsideTrackingMode = OneOutsideTrackingMode.GPS
    ) {
        val db = helper.writableDatabase
        ensureProfile(db, personId)
        db.update(
            TABLE_PROFILES,
            ContentValues().apply {
                put("tracking_enabled", if (enabled) 1 else 0)
                put("mode", mode.wireValue)
                put("updated_at", System.currentTimeMillis())
            },
            "person_id = ?",
            arrayOf(personId.toString())
        )
    }

    @Synchronized
    fun saveHome(personId: UUID, center: OneExteriorPoint, radiusMeters: Double) {
        savePlace(
            personId = personId,
            id = HOME_PLACE_ID,
            kind = PLACE_HOME,
            name = "Home",
            center = center,
            radiusMeters = radiusMeters.coerceAtLeast(ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS)
        )
    }

    @Synchronized
    fun removeHome(personId: UUID) {
        val db = helper.writableDatabase
        db.delete(
            TABLE_PLACES,
            "person_id = ? AND id = ?",
            arrayOf(personId.toString(), HOME_PLACE_ID)
        )
        clearLastZoneIfMatches(db, personId, "home")
    }

    @Synchronized
    fun saveSafePlace(personId: UUID, place: OneExteriorSafePlace) {
        savePlace(
            personId = personId,
            id = place.id,
            kind = PLACE_SAFE,
            name = place.name.trim().ifBlank { "Safe place" },
            center = place.center,
            radiusMeters = place.radiusMeters.coerceAtLeast(ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS)
        )
    }

    @Synchronized
    fun removeSafePlace(personId: UUID, placeId: String) {
        val db = helper.writableDatabase
        db.delete(
            TABLE_PLACES,
            "person_id = ? AND id = ? AND kind = ?",
            arrayOf(personId.toString(), placeId, PLACE_SAFE)
        )
        clearLastZoneIfMatches(db, personId, "safe:$placeId")
    }

    @Synchronized
    fun setResidentMessage(personId: UUID, message: String) {
        val db = helper.writableDatabase
        ensureProfile(db, personId)
        db.update(
            TABLE_PROFILES,
            ContentValues().apply {
                put("resident_message", message.trim().take(240))
                put("updated_at", System.currentTimeMillis())
            },
            "person_id = ?",
            arrayOf(personId.toString())
        )
    }

    @Synchronized
    fun saveProfilePhotoUri(personId: UUID, photoUri: String?) {
        val db = helper.writableDatabase
        ensureProfile(db, personId)
        db.update(
            TABLE_PROFILES,
            ContentValues().apply {
                photoUri?.trim()?.takeIf { it.isNotEmpty() }?.let { put("profile_photo_uri", it) }
                    ?: putNull("profile_photo_uri")
                put("updated_at", System.currentTimeMillis())
            },
            "person_id = ?",
            arrayOf(personId.toString())
        )
    }

    @Synchronized
    fun clearHistory(personId: UUID) {
        val db = helper.writableDatabase
        db.delete(TABLE_POINTS, "person_id = ?", arrayOf(personId.toString()))
        db.delete(TABLE_ALERTS, "person_id = ?", arrayOf(personId.toString()))
        db.update(
            TABLE_PROFILES,
            ContentValues().apply {
                putNull("last_zone_key")
                put("updated_at", System.currentTimeMillis())
            },
            "person_id = ?",
            arrayOf(personId.toString())
        )
    }

    @Synchronized
    fun deletePerson(personId: UUID) {
        val db = helper.writableDatabase
        val id = personId.toString()
        db.delete(TABLE_POINTS, "person_id = ?", arrayOf(id))
        db.delete(TABLE_ALERTS, "person_id = ?", arrayOf(id))
        db.delete(TABLE_PLACES, "person_id = ?", arrayOf(id))
        db.delete(TABLE_PROFILES, "person_id = ?", arrayOf(id))
        if (readSelectedPersonId() == personId) saveSelectedPersonId(null)
    }

    @Synchronized
    fun appendLocation(
        personId: UUID,
        point: OneExteriorPoint,
        accuracyMeters: Float?,
        capturedAtMillis: Long = System.currentTimeMillis(),
        source: OneOutsideLocationSource
    ): OneOutsideLocationResult {
        val db = helper.writableDatabase
        ensureProfile(db, personId)
        var result: OneOutsideLocationResult? = null
        db.transaction {
            val home = readHome(db, personId)
            val safePlaces = readSafePlaces(db, personId)
            val previousZoneKey = readLastZoneKey(db, personId)
            val previousPoint = readLatestPoint(db, personId)
            val zone = classifyOneExteriorPoint(point, home, safePlaces)
            val classified = oneOutsideLocationIsAccurate(accuracyMeters)
            val sameLocation = previousPoint != null &&
                previousPoint.source == source &&
                capturedAtMillis >= previousPoint.capturedAtMillis &&
                point.distanceTo(previousPoint.point) <= ONE_OUTSIDE_STATIONARY_DISTANCE_METERS
            val dwellDurationMillis = if (sameLocation) {
                previousPoint.dwellDurationMillis +
                    (capturedAtMillis - previousPoint.capturedAtMillis).coerceAtLeast(0L)
            } else {
                0L
            }
            val pointRow = OneOutsideLocationPoint(
                personId = personId,
                point = point,
                accuracyMeters = accuracyMeters,
                capturedAtMillis = capturedAtMillis,
                source = source,
                zoneKey = zone.key.takeIf { classified },
                streetName = previousPoint?.streetName.takeIf { sameLocation },
                dwellDurationMillis = dwellDurationMillis
            )
            db.insertOrThrow(
                TABLE_POINTS,
                null,
                ContentValues().apply {
                    put("id", pointRow.id)
                    put("person_id", personId.toString())
                    put("latitude", point.latitude)
                    put("longitude", point.longitude)
                    accuracyMeters?.let { put("accuracy_m", it) } ?: putNull("accuracy_m")
                    put("captured_at", capturedAtMillis)
                    put("source", source.wireValue)
                    pointRow.zoneKey?.let { put("zone_key", it) } ?: putNull("zone_key")
                    pointRow.streetName?.let { put("street_name", it) } ?: putNull("street_name")
                    put("dwell_duration_millis", pointRow.dwellDurationMillis)
                }
            )
            val newAlerts = if (classified) {
                oneOutsideTransitions(previousZoneKey, zone).mapNotNull { type ->
                    val alert = oneOutsideAlert(personId, type, zone, capturedAtMillis)
                    if (hasRecentAlert(db, personId, type, alert.zoneKey, capturedAtMillis)) null
                    else {
                        insertAlert(db, alert)
                        alert
                    }
                }
            } else {
                emptyList()
            }
            if (classified) {
                updateLastZoneKey(db, personId, zone.key, capturedAtMillis)
            }
            prune(db, personId, capturedAtMillis)
            result = OneOutsideLocationResult(pointRow, zone, classified, newAlerts)
        }
        return checkNotNull(result)
    }

    @Synchronized
    fun updateLocationStreetName(
        personId: UUID,
        pointId: String,
        point: OneExteriorPoint,
        capturedAtMillis: Long,
        dwellDurationMillis: Long,
        streetName: String
    ) {
        val normalizedStreet = streetName.trim().take(160)
        if (normalizedStreet.isEmpty()) return
        val db = helper.writableDatabase
        val values = ContentValues().apply { put("street_name", normalizedStreet) }
        db.update(
            TABLE_POINTS,
            values,
            "person_id = ? AND id = ?",
            arrayOf(personId.toString(), pointId)
        )
        if (dwellDurationMillis >= ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS) {
            val coordinateDelta = 0.001
            db.update(
                TABLE_POINTS,
                values,
                "person_id = ? AND captured_at BETWEEN ? AND ? " +
                    "AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?",
                arrayOf(
                    personId.toString(),
                    (capturedAtMillis - dwellDurationMillis).toString(),
                    capturedAtMillis.toString(),
                    (point.latitude - coordinateDelta).toString(),
                    (point.latitude + coordinateDelta).toString(),
                    (point.longitude - coordinateDelta).toString(),
                    (point.longitude + coordinateDelta).toString()
                )
            )
        }
    }

    /**
     * Geofences are a low-power signal. The next reliable GPS sample remains
     * authoritative for the current zone, while this method records a
     * transition immediately when Android wakes the receiver.
     */
    @Synchronized
    fun recordGeofenceSignal(
        personId: UUID,
        placeId: String,
        transition: Int,
        capturedAtMillis: Long = System.currentTimeMillis()
    ): OneOutsideAlert? {
        val db = helper.writableDatabase
        ensureProfile(db, personId)
        if (!readProfile(db, personId).trackingEnabled) return null
        val home = readHome(db, personId)
        val safePlaces = readSafePlaces(db, personId)
        val zone = if (placeId == HOME_PLACE_ID && home != null) {
            OneExteriorZoneMatch("home", OneExteriorZoneKind.HOME, "Home")
        } else {
            safePlaces.firstOrNull { it.id == placeId }?.let {
                OneExteriorZoneMatch("safe:${it.id}", OneExteriorZoneKind.SAFE_PLACE, it.name, it.id)
            }
        } ?: return null
        val type = when {
            transition == GEOFENCE_ENTER && placeId == HOME_PLACE_ID -> OneOutsideAlertType.RETURN_HOME
            transition == GEOFENCE_EXIT && placeId == HOME_PLACE_ID -> OneOutsideAlertType.EXIT_HOME
            transition == GEOFENCE_ENTER && placeId != HOME_PLACE_ID -> OneOutsideAlertType.ENTER_SAFE_PLACE
            else -> return null
        }
        if (hasRecentAlert(db, personId, type, zone.key, capturedAtMillis)) return null
        return oneOutsideAlert(personId, type, zone, capturedAtMillis).also { insertAlert(db, it) }
    }

    private fun savePlace(
        personId: UUID,
        id: String,
        kind: String,
        name: String,
        center: OneExteriorPoint,
        radiusMeters: Double
    ) {
        val db = helper.writableDatabase
        ensureProfile(db, personId)
        db.insertWithOnConflict(
            TABLE_PLACES,
            null,
            ContentValues().apply {
                put("id", id)
                put("person_id", personId.toString())
                put("kind", kind)
                put("name", name)
                put("latitude", center.latitude)
                put("longitude", center.longitude)
                put("radius_m", radiusMeters)
                put("updated_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun readSnapshot(db: SQLiteDatabase, personId: UUID): OneOutsideTrackingSnapshot {
        val profile = readProfile(db, personId)
        val home = readHome(db, personId)
        val safePlaces = readSafePlaces(db, personId)
        val points = readPoints(db, personId)
        val alerts = readAlerts(db, personId)
        return OneOutsideTrackingSnapshot(
            personId = personId,
            trackingEnabled = profile.trackingEnabled,
            mode = OneOutsideTrackingMode.fromWire(profile.mode),
            home = home,
            safePlaces = safePlaces,
            points = points,
            alerts = alerts,
            lastZoneKey = profile.lastZoneKey,
            residentMessage = profile.residentMessage,
            profilePhotoUri = profile.profilePhotoUri
        )
    }

    private fun readProfile(db: SQLiteDatabase, personId: UUID): ProfileRow {
        val cursor = db.query(
            TABLE_PROFILES,
            arrayOf("tracking_enabled", "mode", "last_zone_key", "resident_message", "profile_photo_uri"),
            "person_id = ?",
            arrayOf(personId.toString()),
            null,
            null,
            null,
            "1"
        )
        return cursor.use {
            if (!it.moveToFirst()) ProfileRow()
            else ProfileRow(
                trackingEnabled = it.getInt(0) != 0,
                mode = it.getString(1),
                lastZoneKey = it.getString(2),
                residentMessage = it.getString(3).orEmpty(),
                profilePhotoUri = it.getString(4)
            )
        }
    }

    private fun readHome(db: SQLiteDatabase, personId: UUID): OneExteriorHomeZone? =
        readPlaceCursor(db, personId, PLACE_HOME).firstOrNull()?.let { row ->
            OneExteriorHomeZone(row.center, row.radiusMeters)
        }

    private fun readSafePlaces(db: SQLiteDatabase, personId: UUID): List<OneExteriorSafePlace> =
        readPlaceCursor(db, personId, PLACE_SAFE).map { row ->
            OneExteriorSafePlace(
                id = row.id,
                name = row.name,
                center = row.center,
                radiusMeters = row.radiusMeters
            )
        }

    private fun readPlaceCursor(
        db: SQLiteDatabase,
        personId: UUID,
        kind: String
    ): List<PlaceRow> {
        val cursor = db.query(
            TABLE_PLACES,
            arrayOf("id", "name", "latitude", "longitude", "radius_m"),
            "person_id = ? AND kind = ?",
            arrayOf(personId.toString(), kind),
            null,
            null,
            "name COLLATE NOCASE ASC"
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        PlaceRow(
                            id = it.getString(0),
                            name = it.getString(1),
                            center = OneExteriorPoint(it.getDouble(2), it.getDouble(3)),
                            radiusMeters = it.getDouble(4)
                        )
                    )
                }
            }
        }
    }

    private fun readPoints(db: SQLiteDatabase, personId: UUID): List<OneOutsideLocationPoint> {
        val cursor = db.query(
            TABLE_POINTS,
            arrayOf(
                "id",
                "latitude",
                "longitude",
                "accuracy_m",
                "captured_at",
                "source",
                "zone_key",
                "street_name",
                "dwell_duration_millis"
            ),
            "person_id = ?",
            arrayOf(personId.toString()),
            null,
            null,
            "captured_at DESC",
            ONE_OUTSIDE_MAX_HISTORY_POINTS.toString()
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        OneOutsideLocationPoint(
                            id = it.getString(0),
                            personId = personId,
                            point = OneExteriorPoint(it.getDouble(1), it.getDouble(2)),
                            accuracyMeters = if (it.isNull(3)) null else it.getFloat(3),
                            capturedAtMillis = it.getLong(4),
                            source = OneOutsideLocationSource.fromWire(it.getString(5)),
                            zoneKey = it.getString(6),
                            streetName = it.getString(7),
                            dwellDurationMillis = it.getLong(8)
                        )
                    )
                }
            }.asReversed()
        }
    }

    private fun readLatestPoint(db: SQLiteDatabase, personId: UUID): OneOutsideLocationPoint? {
        val cursor = db.query(
            TABLE_POINTS,
            arrayOf(
                "id",
                "latitude",
                "longitude",
                "accuracy_m",
                "captured_at",
                "source",
                "zone_key",
                "street_name",
                "dwell_duration_millis"
            ),
            "person_id = ?",
            arrayOf(personId.toString()),
            null,
            null,
            "captured_at DESC",
            "1"
        )
        return cursor.use {
            if (!it.moveToFirst()) null
            else OneOutsideLocationPoint(
                id = it.getString(0),
                personId = personId,
                point = OneExteriorPoint(it.getDouble(1), it.getDouble(2)),
                accuracyMeters = if (it.isNull(3)) null else it.getFloat(3),
                capturedAtMillis = it.getLong(4),
                source = OneOutsideLocationSource.fromWire(it.getString(5)),
                zoneKey = it.getString(6),
                streetName = it.getString(7),
                dwellDurationMillis = it.getLong(8)
            )
        }
    }

    private fun readAlerts(db: SQLiteDatabase, personId: UUID): List<OneOutsideAlert> {
        val cursor = db.query(
            TABLE_ALERTS,
            arrayOf("id", "type", "created_at", "title", "detail", "zone_key"),
            "person_id = ?",
            arrayOf(personId.toString()),
            null,
            null,
            "created_at DESC",
            "50"
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        OneOutsideAlert(
                            id = it.getString(0),
                            personId = personId,
                            type = OneOutsideAlertType.fromWire(it.getString(1)),
                            createdAtMillis = it.getLong(2),
                            title = it.getString(3),
                            detail = it.getString(4),
                            zoneKey = it.getString(5)
                        )
                    )
                }
            }.asReversed()
        }
    }

    private fun insertAlert(db: SQLiteDatabase, alert: OneOutsideAlert) {
        db.insertWithOnConflict(
            TABLE_ALERTS,
            null,
            ContentValues().apply {
                put("id", alert.id)
                put("person_id", alert.personId.toString())
                put("type", alert.type.wireValue)
                put("created_at", alert.createdAtMillis)
                put("title", alert.title)
                put("detail", alert.detail)
                alert.zoneKey?.let { put("zone_key", it) } ?: putNull("zone_key")
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    private fun hasRecentAlert(
        db: SQLiteDatabase,
        personId: UUID,
        type: OneOutsideAlertType,
        zoneKey: String?,
        nowMillis: Long
    ): Boolean {
        val where = if (zoneKey == null) {
            "person_id = ? AND type = ? AND zone_key IS NULL AND created_at > ?"
        } else {
            "person_id = ? AND type = ? AND zone_key = ? AND created_at > ?"
        }
        val args = if (zoneKey == null) {
            arrayOf(personId.toString(), type.wireValue, (nowMillis - ALERT_DEDUPLICATION_WINDOW_MILLIS).toString())
        } else {
            arrayOf(personId.toString(), type.wireValue, zoneKey, (nowMillis - ALERT_DEDUPLICATION_WINDOW_MILLIS).toString())
        }
        val cursor = db.query(TABLE_ALERTS, arrayOf("id"), where, args, null, null, null, "1")
        return cursor.use { it.moveToFirst() }
    }

    private fun prune(db: SQLiteDatabase, personId: UUID, nowMillis: Long) {
        val cutoff = nowMillis - ONE_OUTSIDE_MAX_HISTORY_DAYS * 24L * 60L * 60L * 1_000L
        val id = personId.toString()
        db.delete(TABLE_POINTS, "person_id = ? AND captured_at < ?", arrayOf(id, cutoff.toString()))
        db.delete(TABLE_ALERTS, "person_id = ? AND created_at < ?", arrayOf(id, cutoff.toString()))
    }

    private fun ensureProfile(db: SQLiteDatabase, personId: UUID) {
        db.insertWithOnConflict(
            TABLE_PROFILES,
            null,
            ContentValues().apply {
                put("person_id", personId.toString())
                put("tracking_enabled", 0)
                put("mode", OneOutsideTrackingMode.GPS.wireValue)
                put("resident_message", "")
                put("updated_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    private fun readLastZoneKey(db: SQLiteDatabase, personId: UUID): String? =
        readProfile(db, personId).lastZoneKey

    private fun updateLastZoneKey(db: SQLiteDatabase, personId: UUID, key: String, timestamp: Long) {
        db.update(
            TABLE_PROFILES,
            ContentValues().apply {
                put("last_zone_key", key)
                put("updated_at", timestamp)
            },
            "person_id = ?",
            arrayOf(personId.toString())
        )
    }

    private fun clearLastZoneIfMatches(db: SQLiteDatabase, personId: UUID, key: String) {
        db.update(
            TABLE_PROFILES,
            ContentValues().apply {
                putNull("last_zone_key")
                put("updated_at", System.currentTimeMillis())
            },
            "person_id = ? AND last_zone_key = ?",
            arrayOf(personId.toString(), key)
        )
    }

    private data class ProfileRow(
        val trackingEnabled: Boolean = false,
        val mode: String = OneOutsideTrackingMode.GPS.wireValue,
        val lastZoneKey: String? = null,
        val residentMessage: String = "",
        val profilePhotoUri: String? = null
    )

    private data class PlaceRow(
        val id: String,
        val name: String,
        val center: OneExteriorPoint,
        val radiusMeters: Double
    )

    private class OutsideTrackingDatabase(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_METADATA (
                    key TEXT PRIMARY KEY NOT NULL,
                    value TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_PROFILES (
                    person_id TEXT PRIMARY KEY NOT NULL,
                    tracking_enabled INTEGER NOT NULL DEFAULT 0,
                    mode TEXT NOT NULL DEFAULT '${OneOutsideTrackingMode.GPS.wireValue}',
                    last_zone_key TEXT,
                    resident_message TEXT NOT NULL DEFAULT '',
                    profile_photo_uri TEXT,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_PLACES (
                    id TEXT NOT NULL,
                    person_id TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    name TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    radius_m REAL NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (person_id, id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_POINTS (
                    id TEXT PRIMARY KEY NOT NULL,
                    person_id TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    accuracy_m REAL,
                    captured_at INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    zone_key TEXT,
                    street_name TEXT,
                    dwell_duration_millis INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_ALERTS (
                    id TEXT PRIMARY KEY NOT NULL,
                    person_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    detail TEXT NOT NULL,
                    zone_key TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX ${TABLE_PLACES}_person_idx ON $TABLE_PLACES(person_id, kind)")
            db.execSQL("CREATE INDEX ${TABLE_POINTS}_person_time_idx ON $TABLE_POINTS(person_id, captured_at)")
            db.execSQL("CREATE INDEX ${TABLE_ALERTS}_person_time_idx ON $TABLE_ALERTS(person_id, created_at)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 1) {
                onCreate(db)
                return
            }
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE $TABLE_PROFILES ADD COLUMN profile_photo_uri TEXT")
                db.execSQL("ALTER TABLE $TABLE_POINTS ADD COLUMN street_name TEXT")
                db.execSQL("ALTER TABLE $TABLE_POINTS ADD COLUMN dwell_duration_millis INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "one_outside_tracking.db"
        const val DATABASE_VERSION = 2
        const val TABLE_METADATA = "metadata"
        const val TABLE_PROFILES = "profiles"
        const val TABLE_PLACES = "places"
        const val TABLE_POINTS = "location_points"
        const val TABLE_ALERTS = "alerts"
        const val METADATA_SELECTED_PERSON = "selected_person_id"
        const val HOME_PLACE_ID = "home"
        const val PLACE_HOME = "home"
        const val PLACE_SAFE = "safe"
        const val GEOFENCE_ENTER = 1
        const val GEOFENCE_EXIT = 2
        const val ALERT_DEDUPLICATION_WINDOW_MILLIS = 180_000L
    }
}
