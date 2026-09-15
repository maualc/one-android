package com.one.cognitivecompanion

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * A bounded last-known-good cache for the caregiver shell. It deliberately
 * stores derived metadata only (never tokens, frames or audio), so the app can
 * remain useful while connectivity is temporarily unavailable.
 */
class OneOfflineCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /**
     * Remove every cached household snapshot when the local session ends.
     * Cached names, events and map metadata are sensitive even though they do
     * not include access tokens or media bytes.
     */
    // Clear synchronously so a sign-out cannot race with a process death and
    // leave the previous household snapshot on disk.
    @Suppress("UseKtx")
    fun clear() {
        check(preferences.edit().clear().commit()) { "Could not clear the cached ONE household data." }
    }

    fun saveHome(homeId: UUID, snapshot: OneHomeSnapshot) {
        val payload = JSONObject()
            .put("profile", JSONObject()
                .put("home_id", snapshot.profile.homeId.toString())
                .put("home_name", snapshot.profile.homeName)
                .put("resident_name", snapshot.profile.residentName)
                .put("paused", snapshot.profile.paused))
            .put("objects", JSONArray(snapshot.objects.map(::objectJson)))
            .put("events", JSONArray(snapshot.events.map(::eventJson)))
        preferences.edit { putString(key("home", homeId), payload.toString()) }
    }

    fun readHome(homeId: UUID): OneHomeSnapshot? = runCatching {
        val payload = JSONObject(preferences.getString(key("home", homeId), null) ?: return null)
        val profile = payload.getJSONObject("profile")
        OneHomeSnapshot(
            profile = OneHomeProfile(
                homeId = UUID.fromString(profile.getString("home_id")),
                homeName = profile.optString("home_name"),
                residentName = profile.optString("resident_name"),
                paused = profile.optBoolean("paused")
            ),
            objects = payload.optJSONArray("objects").toObjects(),
            events = payload.optJSONArray("events").toEvents()
        )
    }.getOrNull()

    fun saveCameras(homeId: UUID, cameras: List<OneCamera>) {
        preferences.edit { putString(key("cameras", homeId), JSONArray(cameras.map(::cameraJson)).toString()) }
    }

    fun readCameras(homeId: UUID): List<OneCamera> = runCatching {
        JSONArray(preferences.getString(key("cameras", homeId), "[]")).toCameras()
    }.getOrDefault(emptyList())

    fun saveMap(homeId: UUID, rooms: List<OneRoom>, map: OneRoomMap?) {
        val payload = JSONObject()
            .put("rooms", JSONArray(rooms.map { JSONObject().put("id", it.id.toString()).put("home_id", it.homeId?.toString() ?: JSONObject.NULL).put("name", it.name) }))
            .put("map", map?.let(::mapJson) ?: JSONObject.NULL)
        preferences.edit { putString(key("map", homeId), payload.toString()) }
    }

    fun readMap(homeId: UUID): Pair<List<OneRoom>, OneRoomMap?>? = runCatching {
        val payload = JSONObject(preferences.getString(key("map", homeId), null) ?: return null)
        val rooms = buildList {
            val rows = payload.optJSONArray("rooms") ?: JSONArray()
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(OneRoom(UUID.fromString(row.getString("id")), row.optNullableString("home_id")?.let(UUID::fromString), row.optString("name")))
            }
        }
        val map = payload.optJSONObject("map")?.let { row ->
            OneRoomMap(
                id = UUID.fromString(row.getString("id")),
                homeId = row.optString("home_id").takeIf { it.isNotBlank() }?.let(UUID::fromString),
                roomId = row.optNullableString("room_id")?.let(UUID::fromString),
                revision = row.optInt("revision"),
                coordinateFrame = row.optString("coordinate_frame"),
                zones = row.optJSONArray("zones").toStringList(),
                createdAt = row.optString("created_at").toInstantOrNull(),
                source = OneMapSource.fromWire(row.optString("source")),
                provenance = row.optString("provenance").ifBlank { "unknown" },
                dimension = OneMapDimension.fromWire(row.optString("dimension")),
                approximate = row.optBoolean("approximate", true),
                metricScaleKnown = row.optBoolean("metric_scale_known", false),
                scaleMetersPerUnit = row.optNullableDouble("scale_meters_per_unit"),
                localizationStatus = row.optString("localization_status").ifBlank { "unlocalized" },
                geometryStatus = row.optString("geometry_status").ifBlank { "unknown" },
                rescanRequired = row.optBoolean("rescan_required", false),
                confidence = row.optNullableDouble("confidence"),
                modelVersion = row.optNullableString("model_version"),
                polygons = row.optJSONArray("polygons").toMapPolygons(),
                walls = row.optJSONArray("walls").toMapWalls(),
                furniture = row.optJSONArray("furniture").toMapFurniture(),
                openings = row.optJSONArray("openings").toMapOpenings(),
                usdzAvailable = row.optBoolean("usdz_available", false)
            )
        }
        rooms to map
    }.getOrNull()

    private fun key(type: String, homeId: UUID) = "${type}:$homeId"

    private fun objectJson(value: OneRemoteObject) = JSONObject()
        .put("id", value.id.toString()).put("label", value.label).put("status", value.status)
        .put("zone", value.zone ?: JSONObject.NULL).put("x", value.pointX ?: JSONObject.NULL).put("y", value.pointY ?: JSONObject.NULL)
        .put("last_seen_at", value.lastSeenAt?.toString() ?: JSONObject.NULL).put("confidence", value.confidence).put("radius", value.confidenceRadiusM)

    private fun eventJson(value: OneEvent) = JSONObject()
        .put("id", value.id?.toString() ?: JSONObject.NULL).put("kind", value.kind.name).put("location", value.location)
        .put("time", value.time).put("explanation", value.explanation).put("confidence", value.confidence)
        .put("evidence_ids", JSONArray(value.evidenceIds))
        .put("observed_at", value.observedAt?.toString() ?: JSONObject.NULL)

    private fun cameraJson(value: OneCamera) = JSONObject()
        .put("id", value.id.toString()).put("name", value.name).put("room_id", value.roomId?.toString() ?: JSONObject.NULL)
        .put("platform", value.platform).put("status", value.status).put("enabled", value.enabled).put("last_seen_at", value.lastSeenAt?.toString() ?: JSONObject.NULL)

    private fun mapJson(value: OneRoomMap) = JSONObject()
        .put("id", value.id.toString()).put("home_id", value.homeId?.toString() ?: JSONObject.NULL).put("room_id", value.roomId?.toString() ?: JSONObject.NULL)
        .put("revision", value.revision).put("coordinate_frame", value.coordinateFrame).put("zones", JSONArray(value.zones)).put("created_at", value.createdAt?.toString() ?: JSONObject.NULL)
        .put("source", value.source.wireValue).put("provenance", value.provenance).put("dimension", value.dimension.wireValue)
        .put("approximate", value.approximate).put("metric_scale_known", value.metricScaleKnown)
        .put("scale_meters_per_unit", value.scaleMetersPerUnit ?: JSONObject.NULL)
        .put("localization_status", value.localizationStatus).put("geometry_status", value.geometryStatus)
        .put("rescan_required", value.rescanRequired).put("confidence", value.confidence ?: JSONObject.NULL)
        .put("model_version", value.modelVersion ?: JSONObject.NULL)
        .put("polygons", JSONArray(value.polygons.map(::polygonJson)))
        .put("walls", JSONArray(value.walls.map(::wallJson)))
        .put("furniture", JSONArray(value.furniture.map(::furnitureJson)))
        .put("openings", JSONArray(value.openings.map(::openingJson)))
        .put("usdz_available", value.usdzAvailable)

    private fun pointJson(value: OneMapPoint) = JSONObject().put("x", value.x).put("y", value.y)

    private fun polygonJson(value: OneMapPolygon) = JSONObject()
        .put("id", value.id).put("label", value.label)
        .put("points", JSONArray(value.points.map(::pointJson)))
        .put("confidence", value.confidence ?: JSONObject.NULL)

    private fun wallJson(value: OneMapWall) = JSONObject()
        .put("id", value.id).put("start", pointJson(value.start)).put("end", pointJson(value.end))
        .put("confidence", value.confidence ?: JSONObject.NULL)

    private fun furnitureJson(value: OneMapFurniture) = JSONObject()
        .put("id", value.id).put("label", value.label).put("center", pointJson(value.center)).put("size", pointJson(value.size))
        .put("rotation_degrees", value.rotationDegrees).put("confidence", value.confidence ?: JSONObject.NULL)

    private fun openingJson(value: OneMapOpening) = JSONObject()
        .put("id", value.id).put("kind", value.kind).put("start", pointJson(value.start)).put("end", pointJson(value.end))
        .put("confidence", value.confidence ?: JSONObject.NULL)

    private fun JSONArray?.toMapPolygons(): List<OneMapPolygon> = this?.let { rows -> buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val points = row.optJSONArray("points").toMapPoints()
            if (points.size >= 3) add(OneMapPolygon(row.optString("id").ifBlank { "polygon-$index" }, row.optString("label").ifBlank { "Room area" }, points, row.optNullableDouble("confidence")?.toFloat()))
        }
    } } ?: emptyList()

    private fun JSONArray?.toMapWalls(): List<OneMapWall> = this?.let { rows -> buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val start = row.optJSONObject("start").toMapPoint() ?: continue
            val end = row.optJSONObject("end").toMapPoint() ?: continue
            add(OneMapWall(row.optString("id").ifBlank { "wall-$index" }, start, end, row.optNullableDouble("confidence")?.toFloat()))
        }
    } } ?: emptyList()

    private fun JSONArray?.toMapFurniture(): List<OneMapFurniture> = this?.let { rows -> buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val center = row.optJSONObject("center").toMapPoint() ?: continue
            val size = row.optJSONObject("size").toMapPoint() ?: continue
            add(OneMapFurniture(row.optString("id").ifBlank { "furniture-$index" }, row.optString("label").ifBlank { "Furniture" }, center, size, row.optDouble("rotation_degrees", 0.0).toFloat(), row.optNullableDouble("confidence")?.toFloat()))
        }
    } } ?: emptyList()

    private fun JSONArray?.toMapOpenings(): List<OneMapOpening> = this?.let { rows -> buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val start = row.optJSONObject("start").toMapPoint() ?: continue
            val end = row.optJSONObject("end").toMapPoint() ?: continue
            add(OneMapOpening(row.optString("id").ifBlank { "opening-$index" }, row.optString("kind").ifBlank { "opening" }, start, end, row.optNullableDouble("confidence")?.toFloat()))
        }
    } } ?: emptyList()

    private fun JSONArray?.toMapPoints(): List<OneMapPoint> = this?.let { rows -> buildList {
        for (index in 0 until rows.length()) rows.optJSONObject(index).toMapPoint()?.let(::add)
    } } ?: emptyList()

    private fun JSONObject?.toMapPoint(): OneMapPoint? {
        val row = this ?: return null
        val x = row.optDouble("x", Double.NaN)
        val y = row.optDouble("y", Double.NaN)
        return if (x.isFinite() && y.isFinite()) OneMapPoint(x.toFloat(), y.toFloat()) else null
    }

    private fun JSONArray?.toObjects(): List<OneRemoteObject> = this?.let { rows -> buildList { for (index in 0 until rows.length()) runCatching { rows.getJSONObject(index) }.getOrNull()?.let { row -> runCatching { add(OneRemoteObject(UUID.fromString(row.getString("id")), row.optString("label"), row.optString("status"), row.optNullableString("zone"), row.optNullableDouble("x"), row.optNullableDouble("y"), row.optNullableString("last_seen_at").toInstantOrNull(), row.optDouble("confidence"), row.optDouble("radius"))) } } } } ?: emptyList()
    private fun JSONArray?.toEvents(): List<OneEvent> = this?.let { rows -> buildList { for (index in 0 until rows.length()) runCatching { rows.getJSONObject(index) }.getOrNull()?.let { row -> runCatching { add(OneEvent(runCatching { EventKind.valueOf(row.optString("kind")) }.getOrDefault(EventKind.OTHER), row.optString("location"), row.optString("time"), row.optString("explanation"), row.optString("confidence"), row.optNullableString("id")?.let(UUID::fromString), row.optNullableString("observed_at").toInstantOrNull(), row.optJSONArray("evidence_ids").toStringList())) } } } } ?: emptyList()
    private fun JSONArray?.toCameras(): List<OneCamera> = this?.let { rows -> buildList { for (index in 0 until rows.length()) runCatching { rows.getJSONObject(index) }.getOrNull()?.let { row -> runCatching { add(OneCamera(UUID.fromString(row.getString("id")), row.optString("name"), row.optNullableString("room_id")?.let(UUID::fromString), row.optString("platform"), row.optString("status"), row.optBoolean("enabled", true), row.optNullableString("last_seen_at").toInstantOrNull())) } } } } ?: emptyList()
    private fun JSONArray?.toStringList(): List<String> = this?.let { rows -> buildList { for (index in 0 until rows.length()) rows.optString(index).takeIf { it.isNotBlank() }?.let(::add) } } ?: emptyList()

    private fun JSONObject.optNullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }
    private fun JSONObject.optNullableString(key: String): String? = optString(key).takeIf { it.isNotBlank() && it != "null" }
    private fun String?.toInstantOrNull(): Instant? = this?.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private companion object { const val PREFERENCES = "one.offline.cache" }
}
