package com.one.cognitivecompanion

import org.json.JSONArray
import org.json.JSONObject

data class OneMapImport(
    val roomName: String,
    val zones: List<String>,
    val coordinateFrame: String = "android-import",
    val source: OneMapSource = OneMapSource.UNKNOWN,
    val dimension: OneMapDimension = OneMapDimension.UNKNOWN,
    val rescanRequired: Boolean = false,
    val geometryItemCount: Int = 0
)

/** Parses the portable map format used by the Android fallback importer. */
fun parseOneMapImport(json: String): OneMapImport {
    val body = JSONObject(json)
    val mapData = body.optJSONObject("map_data")
    val geometry = body.optJSONObject("geometry") ?: mapData?.optJSONObject("geometry")
    val roomName = body.optString("room_name")
        .ifBlank { body.optString("roomName") }
        .ifBlank { mapData?.optString("room_name").orEmpty() }
        .ifBlank { "Imported room" }
    val source = OneMapSource.fromWire(body.optString("source").ifBlank { mapData?.optString("source").orEmpty() })
    val dimension = OneMapDimension.fromWire(body.optString("dimension").ifBlank { mapData?.optString("dimension").orEmpty() })
    val coordinateFrame = body.optString("coordinate_frame")
        .ifBlank { body.optString("coordinateFrame") }
        .ifBlank { mapData?.optString("coordinate_frame").orEmpty() }
        .ifBlank { if (source == OneMapSource.ROOMPLAN_LIDAR_3D) "roomplan-local" else "android-import" }
    val rawZones = body.optJSONArray("zones")
        ?: mapData?.optJSONArray("zones")
        ?: geometry?.optJSONArray("zones")
        ?: geometry?.optJSONArray("room_zones")
        ?: geometry?.optJSONArray("polygons")
        ?: JSONArray()
    val zones = buildList {
        for (index in 0 until rawZones.length()) {
            when (val value = rawZones.opt(index)) {
                is JSONObject -> value.optString("label").ifBlank { value.optString("name") }.takeIf { it.isNotBlank() }?.let(::add)
                is String -> value.trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }.distinct().take(100)
    require(zones.isNotEmpty()) { "El mapa debe incluir al menos una zona." }
    val geometryItemCount = listOf("polygons", "walls", "furniture", "openings")
        .sumOf { key -> geometry?.optJSONArray(key)?.length() ?: 0 }
    return OneMapImport(
        roomName = roomName.trim().take(120),
        zones = zones,
        coordinateFrame = coordinateFrame.trim().take(80),
        source = source,
        dimension = dimension,
        rescanRequired = body.optBoolean("rescan_required", source == OneMapSource.LEGACY_2D),
        geometryItemCount = geometryItemCount
    )
}
