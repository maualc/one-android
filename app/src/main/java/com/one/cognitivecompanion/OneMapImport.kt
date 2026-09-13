package com.one.cognitivecompanion

import org.json.JSONArray
import org.json.JSONObject

data class OneMapImport(
    val roomName: String,
    val zones: List<String>,
    val coordinateFrame: String = "android-import"
)

/** Parses the portable map format used by the Android fallback importer. */
fun parseOneMapImport(json: String): OneMapImport {
    val body = JSONObject(json)
    val roomName = body.optString("room_name").ifBlank { body.optString("roomName") }.ifBlank { "Imported room" }
    val coordinateFrame = body.optString("coordinate_frame").ifBlank { body.optString("coordinateFrame") }.ifBlank { "android-import" }
    val rawZones = body.optJSONArray("zones") ?: body.optJSONObject("map_data")?.optJSONArray("zones") ?: JSONArray()
    val zones = buildList {
        for (index in 0 until rawZones.length()) {
            when (val value = rawZones.opt(index)) {
                is JSONObject -> value.optString("label").takeIf { it.isNotBlank() }?.let(::add)
                is String -> value.trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }.distinct().take(100)
    require(zones.isNotEmpty()) { "El mapa debe incluir al menos una zona." }
    return OneMapImport(roomName.trim().take(120), zones, coordinateFrame.trim().take(80))
}

