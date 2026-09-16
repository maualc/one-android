package com.one.cognitivecompanion

import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic primitives shared by the outside companion map and route
 * simulator. They intentionally do not share the backend's indoor-map types.
 */
data class OneExteriorPoint(
    val latitude: Double,
    val longitude: Double
)

data class OneExteriorHomeZone(
    val center: OneExteriorPoint,
    val radiusMeters: Double = ONE_EXTERIOR_DEFAULT_HOME_RADIUS_METERS
)

data class OneExteriorSafePlace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val center: OneExteriorPoint,
    val radiusMeters: Double = ONE_EXTERIOR_DEFAULT_SAFE_RADIUS_METERS
)

enum class OneExteriorZoneKind {
    HOME,
    SAFE_PLACE,
    OUTSIDE
}

data class OneExteriorZoneMatch(
    val key: String,
    val kind: OneExteriorZoneKind,
    val label: String,
    val safePlaceId: String? = null
)


data class OneExteriorRouteTemplate(
    val id: String,
    val label: String,
    val description: String
)

val oneExteriorRouteTemplates = listOf(
    OneExteriorRouteTemplate(
        id = ONE_EXTERIOR_ROUTE_NEIGHBORHOOD,
        label = "Neighbourhood walk",
        description = "Leaves home, stays outside the zones, and returns."
    ),
    OneExteriorRouteTemplate(
        id = ONE_EXTERIOR_ROUTE_SAFE_PLACE,
        label = "Walk to a safe place",
        description = "Leaves home, reaches a configured safe place, then returns."
    ),
    OneExteriorRouteTemplate(
        id = ONE_EXTERIOR_ROUTE_LONG,
        label = "Longer outing",
        description = "A longer simulated route with several outdoor steps."
    )
)

const val ONE_EXTERIOR_ROUTE_NEIGHBORHOOD = "neighbourhood"
const val ONE_EXTERIOR_ROUTE_SAFE_PLACE = "safe_place"
const val ONE_EXTERIOR_ROUTE_LONG = "long_outing"
const val ONE_EXTERIOR_DEFAULT_HOME_RADIUS_METERS = 150.0
const val ONE_EXTERIOR_DEFAULT_SAFE_RADIUS_METERS = 100.0

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun OneExteriorPoint.distanceTo(other: OneExteriorPoint): Double {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val deltaLat = Math.toRadians(other.latitude - latitude)
    val deltaLon = Math.toRadians(other.longitude - longitude)
    val a = sin(deltaLat / 2.0) * sin(deltaLat / 2.0) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2.0) * sin(deltaLon / 2.0)
    return EARTH_RADIUS_METERS * 2.0 * kotlin.math.atan2(sqrt(a), sqrt(1.0 - a))
}

fun OneExteriorPoint.offsetMeters(eastMeters: Double, northMeters: Double): OneExteriorPoint {
    val latitudeRadians = Math.toRadians(latitude)
    val nextLatitude = latitude + Math.toDegrees(northMeters / EARTH_RADIUS_METERS)
    val longitudeScale = (EARTH_RADIUS_METERS * cos(latitudeRadians)).coerceAtLeast(1.0)
    val nextLongitude = longitude + Math.toDegrees(eastMeters / longitudeScale)
    return OneExteriorPoint(nextLatitude, nextLongitude)
}

fun classifyOneExteriorPoint(
    point: OneExteriorPoint,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>
): OneExteriorZoneMatch {
    if (home != null && point.distanceTo(home.center) <= home.radiusMeters.coerceAtLeast(1.0)) {
        return OneExteriorZoneMatch("home", OneExteriorZoneKind.HOME, "Home")
    }
    val safePlace = safePlaces
        .map { it to point.distanceTo(it.center) }
        .filter { (place, distance) -> distance <= place.radiusMeters.coerceAtLeast(1.0) }
        .minByOrNull { it.second }
        ?.first
    if (safePlace != null) {
        return OneExteriorZoneMatch(
            key = "safe:${safePlace.id}",
            kind = OneExteriorZoneKind.SAFE_PLACE,
            label = safePlace.name,
            safePlaceId = safePlace.id
        )
    }
    return OneExteriorZoneMatch("outside", OneExteriorZoneKind.OUTSIDE, "Outside configured zones")
}


fun routePointsForOneExteriorDemo(
    routeId: String,
    home: OneExteriorHomeZone,
    safePlaces: List<OneExteriorSafePlace>
): List<OneExteriorPoint> {
    val origin = home.center
    val firstSafePlace = safePlaces.firstOrNull()
    return when (routeId) {
        ONE_EXTERIOR_ROUTE_SAFE_PLACE -> buildList {
            add(origin)
            add(origin.offsetMeters(eastMeters = 125.0, northMeters = 20.0))
            add(firstSafePlace?.center ?: origin.offsetMeters(eastMeters = 240.0, northMeters = 80.0))
            add(origin.offsetMeters(eastMeters = 120.0, northMeters = -40.0))
            add(origin)
        }
        ONE_EXTERIOR_ROUTE_LONG -> listOf(
            origin,
            origin.offsetMeters(eastMeters = 130.0, northMeters = 30.0),
            origin.offsetMeters(eastMeters = 300.0, northMeters = 170.0),
            firstSafePlace?.center ?: origin.offsetMeters(eastMeters = -240.0, northMeters = 190.0),
            origin.offsetMeters(eastMeters = -120.0, northMeters = 60.0),
            origin
        )
        else -> listOf(
            origin,
            origin.offsetMeters(eastMeters = 120.0, northMeters = 20.0),
            origin.offsetMeters(eastMeters = 220.0, northMeters = 90.0),
            origin.offsetMeters(eastMeters = 80.0, northMeters = -110.0),
            origin
        )
    }
}
