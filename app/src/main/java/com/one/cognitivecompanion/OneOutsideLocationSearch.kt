package com.one.cognitivecompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OneOutsideLocationSearchResult(
    val displayName: String,
    val point: OneExteriorPoint
)

private const val ONE_NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
private const val ONE_OUTSIDE_SEARCH_USER_AGENT = "ONE Android outside companion/1.0 (OpenStreetMap search)"
private const val ONE_OUTSIDE_SEARCH_TIMEOUT_MILLIS = 10_000
private const val ONE_OUTSIDE_SEARCH_MIN_INTERVAL_MILLIS = 1_000L

private val outsideSearchLock = Any()
private var outsideLastSearchAtMillis = 0L

suspend fun searchOneOutsideLocations(query: String): List<OneOutsideLocationSearchResult> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()

    respectOutsideSearchRateLimit()
    return withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(normalizedQuery, Charsets.UTF_8.name())
        val connection = (URL(
            ONE_NOMINATIM_SEARCH_URL +
                "?format=jsonv2&limit=5&addressdetails=1&q=" + encodedQuery
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = ONE_OUTSIDE_SEARCH_TIMEOUT_MILLIS
            readTimeout = ONE_OUTSIDE_SEARCH_TIMEOUT_MILLIS
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            setRequestProperty("User-Agent", ONE_OUTSIDE_SEARCH_USER_AGENT)
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Location search returned HTTP ${connection.responseCode}.")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            parseOneOutsideLocationSearchResults(payload)
        } finally {
            connection.disconnect()
        }
    }
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

private suspend fun respectOutsideSearchRateLimit() {
    val waitMillis = synchronized(outsideSearchLock) {
        (ONE_OUTSIDE_SEARCH_MIN_INTERVAL_MILLIS -
            (System.currentTimeMillis() - outsideLastSearchAtMillis)).coerceAtLeast(0L)
    }
    if (waitMillis > 0L) delay(waitMillis)
    synchronized(outsideSearchLock) {
        outsideLastSearchAtMillis = System.currentTimeMillis()
    }
}
