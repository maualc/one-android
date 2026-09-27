package com.one.cognitivecompanion

import org.json.JSONArray
import java.util.UUID

data class OneOutsideLocationSearchResult(
    val displayName: String,
    val point: OneExteriorPoint
)

suspend fun searchOneOutsideLocations(query: String, session: OneSession, recipientId: UUID): List<OneOutsideLocationSearchResult> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()
    return parseOneOutsideLocationSearchResults(OneHttpApiClient(RuntimeConfiguration()).outsideLocationSearch(session, recipientId, normalizedQuery))
}

suspend fun reverseGeocodeOneOutsideLocation(point: OneExteriorPoint, session: OneSession, recipientId: UUID): String? {
    return OneHttpApiClient(RuntimeConfiguration()).outsideLocationReverse(session, recipientId, point)?.let(::parseOneOutsideReverseGeocode)
}

internal fun parseOneOutsideLocationSearchResults(payload: String): List<OneOutsideLocationSearchResult> {
    val results = JSONArray(payload)
    return buildList(results.length()) {
        for (index in 0 until results.length()) {
            val result = results.optJSONObject(index) ?: continue
            val displayName = result.optString("display_name").trim()
            val latitude = result.optString("lat").toDoubleOrNull()
            val longitude = result.optString("lon").toDoubleOrNull()
            if (displayName.isNotEmpty() && latitude != null && longitude != null) {
                add(
                    OneOutsideLocationSearchResult(
                        displayName = displayName,
                        point = OneExteriorPoint(latitude, longitude)
                    )
                )
            }
        }
    }
}

internal fun parseOneOutsideReverseGeocode(payload: String): String? {
    val result = org.json.JSONObject(payload)
    val address = result.optJSONObject("address")
    val road = address?.let {
        listOf("road", "pedestrian", "footway", "path")
            .firstNotNullOfOrNull { key -> it.optString(key).trim().takeIf(String::isNotEmpty) }
    }
    val houseNumber = address?.optString("house_number")?.trim().orEmpty()
    val locality = address?.let {
        listOf("city", "town", "village", "municipality", "suburb")
            .firstNotNullOfOrNull { key -> it.optString(key).trim().takeIf(String::isNotEmpty) }
    }.orEmpty()
    return when {
        road != null && houseNumber.isNotEmpty() && locality.isNotEmpty() -> "$road $houseNumber, $locality"
        road != null && houseNumber.isNotEmpty() -> "$road $houseNumber"
        road != null && locality.isNotEmpty() -> "$road, $locality"
        road != null -> road
        else -> result.optString("display_name").trim().takeIf { it.isNotEmpty() }?.let { name ->
            name.split(',').take(3).joinToString(", ").trim()
        }
    }
}
