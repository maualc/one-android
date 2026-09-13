package com.one.cognitivecompanion

import android.content.Context
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

    fun saveHome(homeId: UUID, snapshot: OneHomeSnapshot) {
        val payload = JSONObject()
            .put("profile", JSONObject()
                .put("home_id", snapshot.profile.homeId.toString())
                .put("home_name", snapshot.profile.homeName)
                .put("resident_name", snapshot.profile.residentName)
                .put("paused", snapshot.profile.paused))
            .put("objects", JSONArray(snapshot.objects.map(::objectJson)))
            .put("events", JSONArray(snapshot.events.map(::eventJson)))
        preferences.edit().putString(key("home", homeId), payload.toString()).apply()
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
        preferences.edit().putString(key("cameras", homeId), JSONArray(cameras.map(::cameraJson)).toString()).apply()
    }

    fun readCameras(homeId: UUID): List<OneCamera> = runCatching {
        JSONArray(preferences.getString(key("cameras", homeId), "[]")).toCameras()
    }.getOrDefault(emptyList())

    fun saveMap(homeId: UUID, rooms: List<OneRoom>, map: OneRoomMap?) {
        val payload = JSONObject()
            .put("rooms", JSONArray(rooms.map { JSONObject().put("id", it.id.toString()).put("home_id", it.homeId?.toString() ?: JSONObject.NULL).put("name", it.name) }))
            .put("map", map?.let(::mapJson) ?: JSONObject.NULL)
        preferences.edit().putString(key("map", homeId), payload.toString()).apply()
    }

    fun readMap(homeId: UUID): Pair<List<OneRoom>, OneRoomMap?>? = runCatching {
        val payload = JSONObject(preferences.getString(key("map", homeId), null) ?: return null)
        val rooms = buildList {
            val rows = payload.optJSONArray("rooms") ?: JSONArray()
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(OneRoom(UUID.fromString(row.getString("id")), row.optString("home_id").takeIf { it.isNotBlank() }?.let(UUID::fromString), row.optString("name")))
            }
        }
        val map = payload.optJSONObject("map")?.let { row ->
            OneRoomMap(
                id = UUID.fromString(row.getString("id")),
                homeId = row.optString("home_id").takeIf { it.isNotBlank() }?.let(UUID::fromString),
                roomId = row.optString("room_id").takeIf { it.isNotBlank() }?.let(UUID::fromString),
                revision = row.optInt("revision"),
                coordinateFrame = row.optString("coordinate_frame"),
                zones = row.optJSONArray("zones").toStringList(),
                createdAt = row.optString("created_at").toInstantOrNull()
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
        .put("observed_at", value.observedAt?.toString() ?: JSONObject.NULL)

    private fun cameraJson(value: OneCamera) = JSONObject()
        .put("id", value.id.toString()).put("name", value.name).put("room_id", value.roomId?.toString() ?: JSONObject.NULL)
        .put("platform", value.platform).put("status", value.status).put("enabled", value.enabled).put("last_seen_at", value.lastSeenAt?.toString() ?: JSONObject.NULL)

    private fun mapJson(value: OneRoomMap) = JSONObject()
        .put("id", value.id.toString()).put("home_id", value.homeId?.toString() ?: JSONObject.NULL).put("room_id", value.roomId?.toString() ?: JSONObject.NULL)
        .put("revision", value.revision).put("coordinate_frame", value.coordinateFrame).put("zones", JSONArray(value.zones)).put("created_at", value.createdAt?.toString() ?: JSONObject.NULL)

    private fun JSONArray?.toObjects(): List<OneRemoteObject> = this?.let { rows -> buildList { for (index in 0 until rows.length()) runCatching { rows.getJSONObject(index) }.getOrNull()?.let { row -> runCatching { add(OneRemoteObject(UUID.fromString(row.getString("id")), row.optString("label"), row.optString("status"), row.optString("zone").takeIf { it.isNotBlank() }, row.optNullableDouble("x"), row.optNullableDouble("y"), row.optString("last_seen_at").toInstantOrNull(), row.optDouble("confidence"), row.optDouble("radius"))) } } } } ?: emptyList()
    private fun JSONArray?.toEvents(): List<OneEvent> = this?.let { rows -> buildList { for (index in 0 until rows.length()) runCatching { rows.getJSONObject(index) }.getOrNull()?.let { row -> runCatching { add(OneEvent(EventKind.valueOf(row.optString("kind")), row.optString("location"), row.optString("time"), row.optString("explanation"), row.optString("confidence"), row.optString("id").takeIf { it.isNotBlank() }?.let(UUID::fromString), row.optString("observed_at").toInstantOrNull())) } } } } ?: emptyList()
    private fun JSONArray?.toCameras(): List<OneCamera> = this?.let { rows -> buildList { for (index in 0 until rows.length()) runCatching { rows.getJSONObject(index) }.getOrNull()?.let { row -> runCatching { add(OneCamera(UUID.fromString(row.getString("id")), row.optString("name"), row.optString("room_id").takeIf { it.isNotBlank() }?.let(UUID::fromString), row.optString("platform"), row.optString("status"), row.optBoolean("enabled", true), row.optString("last_seen_at").toInstantOrNull())) } } } } ?: emptyList()
    private fun JSONArray?.toStringList(): List<String> = this?.let { rows -> buildList { for (index in 0 until rows.length()) rows.optString(index).takeIf { it.isNotBlank() }?.let(::add) } } ?: emptyList()

    private fun JSONObject.optNullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }
    private fun String?.toInstantOrNull(): Instant? = this?.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private companion object { const val PREFERENCES = "one.offline.cache" }
}

