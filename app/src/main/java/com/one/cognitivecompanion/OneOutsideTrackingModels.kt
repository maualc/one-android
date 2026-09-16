package com.one.cognitivecompanion

import java.util.UUID

const val ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS = 150.0
const val ONE_OUTSIDE_DEFAULT_SAFE_RADIUS_METERS = 100.0
const val ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS = 100.0
const val ONE_OUTSIDE_MAX_HISTORY_DAYS = 7L
const val ONE_OUTSIDE_MAX_HISTORY_POINTS = 10_000
const val ONE_OUTSIDE_LOCATION_INTERVAL_MILLIS = 120_000L
const val ONE_OUTSIDE_MIN_LOCATION_INTERVAL_MILLIS = 60_000L
const val ONE_OUTSIDE_MIN_LOCATION_DISTANCE_METERS = 50f
const val ONE_OUTSIDE_MAX_CLASSIFICATION_ACCURACY_METERS = 120f

enum class OneOutsideLocationSource(val wireValue: String) {
    GPS("gps"),
    SIMULATED("simulated");

    companion object {
        fun fromWire(value: String?): OneOutsideLocationSource =
            entries.firstOrNull { it.wireValue == value } ?: GPS
    }
}

enum class OneOutsideTrackingMode(val wireValue: String) {
    GPS("gps"),
    SIMULATED("simulated");

    companion object {
        fun fromWire(value: String?): OneOutsideTrackingMode =
            entries.firstOrNull { it.wireValue == value } ?: GPS
    }
}

enum class OneOutsideAlertType(val wireValue: String) {
    EXIT_HOME("exit_home"),
    RETURN_HOME("return_home"),
    ENTER_SAFE_PLACE("enter_safe_place"),
    OUTSIDE_ALL_ZONES("outside_all_zones");

    companion object {
        fun fromWire(value: String?): OneOutsideAlertType =
            entries.firstOrNull { it.wireValue == value } ?: OUTSIDE_ALL_ZONES
    }
}

data class OneOutsideLocationPoint(
    val id: String = UUID.randomUUID().toString(),
    val personId: UUID,
    val point: OneExteriorPoint,
    val accuracyMeters: Float?,
    val capturedAtMillis: Long,
    val source: OneOutsideLocationSource,
    val zoneKey: String?
)

data class OneOutsideAlert(
    val id: String = UUID.randomUUID().toString(),
    val personId: UUID,
    val type: OneOutsideAlertType,
    val createdAtMillis: Long,
    val title: String,
    val detail: String,
    val zoneKey: String?
)

data class OneOutsideLocationResult(
    val point: OneOutsideLocationPoint,
    val zone: OneExteriorZoneMatch,
    val classified: Boolean,
    val alerts: List<OneOutsideAlert>
)

data class OneOutsideTrackingSnapshot(
    val personId: UUID,
    val trackingEnabled: Boolean = false,
    val mode: OneOutsideTrackingMode = OneOutsideTrackingMode.GPS,
    val home: OneExteriorHomeZone? = null,
    val safePlaces: List<OneExteriorSafePlace> = emptyList(),
    val points: List<OneOutsideLocationPoint> = emptyList(),
    val alerts: List<OneOutsideAlert> = emptyList(),
    val lastZoneKey: String? = null,
    val residentMessage: String = ""
) {
    val lastPoint: OneOutsideLocationPoint?
        get() = points.lastOrNull()

    val currentZone: OneExteriorZoneMatch?
        get() = lastPoint?.zoneKey?.let { key ->
            outsideZoneForKey(key, home, safePlaces)
        } ?: lastZoneKey?.let { key ->
            outsideZoneForKey(key, home, safePlaces)
        }
}

fun outsideZoneForKey(
    key: String,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>
): OneExteriorZoneMatch = when {
    key == "home" && home != null ->
        OneExteriorZoneMatch("home", OneExteriorZoneKind.HOME, "Home")
    key.startsWith("safe:") -> {
        val id = key.removePrefix("safe:")
        safePlaces.firstOrNull { it.id == id }?.let { place ->
            OneExteriorZoneMatch(key, OneExteriorZoneKind.SAFE_PLACE, place.name, place.id)
        } ?: OneExteriorZoneMatch("outside", OneExteriorZoneKind.OUTSIDE, "Outside configured zones")
    }
    else -> OneExteriorZoneMatch("outside", OneExteriorZoneKind.OUTSIDE, "Outside configured zones")
}

fun oneOutsideAlert(
    personId: UUID,
    type: OneOutsideAlertType,
    zone: OneExteriorZoneMatch,
    capturedAtMillis: Long
): OneOutsideAlert {
    val (title, detail) = when (type) {
        OneOutsideAlertType.EXIT_HOME ->
            "Left home" to "The location service detected that the person left the home zone."
        OneOutsideAlertType.RETURN_HOME ->
            "Returned home" to "The location service detected a return to the home zone."
        OneOutsideAlertType.ENTER_SAFE_PLACE ->
            "Entered ${zone.label}" to "The person entered the configured safe place."
        OneOutsideAlertType.OUTSIDE_ALL_ZONES ->
            "Outside configured zones" to "The latest reliable location is outside home and every safe place."
    }
    return OneOutsideAlert(
        personId = personId,
        type = type,
        createdAtMillis = capturedAtMillis,
        title = title,
        detail = detail,
        zoneKey = zone.key
    )
}

fun oneOutsideTransitions(
    previousZoneKey: String?,
    currentZone: OneExteriorZoneMatch
): List<OneOutsideAlertType> {
    if (previousZoneKey == null || previousZoneKey == currentZone.key) return emptyList()
    return buildList {
        if (previousZoneKey == "home" && currentZone.key != "home") {
            add(OneOutsideAlertType.EXIT_HOME)
        } else if (previousZoneKey != "home" && currentZone.key == "home") {
            add(OneOutsideAlertType.RETURN_HOME)
        }
        if (currentZone.kind == OneExteriorZoneKind.SAFE_PLACE && previousZoneKey != currentZone.key) {
            add(OneOutsideAlertType.ENTER_SAFE_PLACE)
        }
        if (currentZone.kind == OneExteriorZoneKind.OUTSIDE &&
            previousZoneKey != "home" &&
            previousZoneKey != "outside"
        ) {
            add(OneOutsideAlertType.OUTSIDE_ALL_ZONES)
        }
    }
}

fun oneOutsideRoutePoints(
    routeId: String,
    home: OneExteriorHomeZone,
    safePlaces: List<OneExteriorSafePlace>
): List<OneExteriorPoint> = routePointsForOneExteriorDemo(routeId, home, safePlaces)

fun oneOutsideLocationIsAccurate(accuracyMeters: Float?): Boolean =
    accuracyMeters == null || accuracyMeters <= ONE_OUTSIDE_MAX_CLASSIFICATION_ACCURACY_METERS
