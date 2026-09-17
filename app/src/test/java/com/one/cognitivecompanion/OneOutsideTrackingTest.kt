package com.one.cognitivecompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OneOutsideTrackingTest {
    private val personId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val home = OneExteriorHomeZone(
        center = OneExteriorPoint(40.4168, -3.7038),
        radiusMeters = 150.0
    )
    private val safePlace = OneExteriorSafePlace(
        id = "garden",
        name = "Community garden",
        center = home.center.offsetMeters(eastMeters = 300.0, northMeters = 0.0),
        radiusMeters = 100.0
    )

    @Test
    fun outsideZonesStartAtTwentyMetresAndCanBeConfiguredLarger() {
        assertEquals(20.0, ONE_OUTSIDE_DEFAULT_HOME_RADIUS_METERS, 0.0)
        assertEquals(20.0, ONE_OUTSIDE_DEFAULT_SAFE_RADIUS_METERS, 0.0)
        assertEquals(20.0, ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS, 0.0)
        assertEquals(20.0, OneExteriorHomeZone(home.center).radiusMeters, 0.0)
        assertEquals(250.0, OneExteriorHomeZone(home.center, 250.0).radiusMeters, 0.0)
    }

    @Test
    fun safePlaceRouteUsesSelectedOriginAndDestination() {
        val secondSafePlace = OneExteriorSafePlace(
            id = "pharmacy",
            name = "Pharmacy",
            center = home.center.offsetMeters(eastMeters = -220.0, northMeters = 180.0),
            radiusMeters = 30.0
        )
        val origins = oneExteriorRouteOriginOptions(
            home = home,
            currentPoint = home.center.offsetMeters(eastMeters = 40.0, northMeters = -30.0),
            safePlaces = listOf(safePlace, secondSafePlace)
        )
        assertEquals(
            listOf(
                OneExteriorRouteEndpointKind.HOME,
                OneExteriorRouteEndpointKind.CURRENT_LOCATION,
                OneExteriorRouteEndpointKind.SAFE_PLACE,
                OneExteriorRouteEndpointKind.SAFE_PLACE
            ),
            origins.map { it.kind }
        )

        val route = oneOutsideRoutePoints(
            routeId = ONE_EXTERIOR_ROUTE_SAFE_PLACE,
            home = home,
            safePlaces = listOf(safePlace, secondSafePlace),
            originPoint = safePlace.center,
            destinationPoint = secondSafePlace.center
        )
        assertEquals(safePlace.center, route.first())
        assertEquals(secondSafePlace.center, route.last())
        assertTrue(route.size >= 4)
    }

    @Test
    fun zoneTransitionsEmitOnlyMeaningfulAlerts() {
        val homeZone = outsideZoneForKey("home", home, listOf(safePlace))
        val outsideZone = outsideZoneForKey("outside", home, listOf(safePlace))
        val safeZone = outsideZoneForKey("safe:garden", home, listOf(safePlace))

        assertTrue(oneOutsideTransitions(null, homeZone).isEmpty())
        assertEquals(
            listOf(OneOutsideAlertType.EXIT_HOME),
            oneOutsideTransitions("home", outsideZone)
        )
        assertEquals(
            listOf(OneOutsideAlertType.RETURN_HOME),
            oneOutsideTransitions("outside", homeZone)
        )
        assertEquals(
            listOf(OneOutsideAlertType.EXIT_HOME, OneOutsideAlertType.ENTER_SAFE_PLACE),
            oneOutsideTransitions("home", safeZone)
        )
        assertEquals(
            listOf(OneOutsideAlertType.OUTSIDE_ALL_ZONES),
            oneOutsideTransitions("safe:garden", outsideZone)
        )
        assertTrue(oneOutsideTransitions("outside", outsideZone).isEmpty())
    }

    @Test
    fun inaccurateLocationIsStoredButCannotClassifyAZone() {
        assertTrue(oneOutsideLocationIsAccurate(null))
        assertTrue(oneOutsideLocationIsAccurate(120f))
        assertFalse(oneOutsideLocationIsAccurate(120.1f))
    }

    @Test
    fun streetNameReplacesGenericOutsideLabelAndDwellHasFiveMinuteThreshold() {
        val outsidePoint = OneOutsideLocationPoint(
            personId = personId,
            point = home.center.offsetMeters(eastMeters = 500.0, northMeters = 0.0),
            accuracyMeters = 8f,
            capturedAtMillis = 1L,
            source = OneOutsideLocationSource.GPS,
            zoneKey = "outside"
        )
        assertEquals("Street not available yet", oneOutsideLocationLabel(outsidePoint, home, listOf(safePlace)))
        assertEquals(
            "Carrer de la Prova",
            oneOutsideLocationLabel(outsidePoint.copy(streetName = "Carrer de la Prova"), home, listOf(safePlace))
        )
        assertTrue(ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS == 5L * 60L * 1_000L)
    }

    @Test
    fun snapshotUsesLatestReliableZoneForCurrentSituation() {
        val snapshot = OneOutsideTrackingSnapshot(
            personId = personId,
            home = home,
            safePlaces = listOf(safePlace),
            lastZoneKey = "home",
            points = listOf(
                OneOutsideLocationPoint(
                    personId = personId,
                    point = home.center,
                    accuracyMeters = 8f,
                    capturedAtMillis = 1L,
                    source = OneOutsideLocationSource.GPS,
                    zoneKey = "home"
                )
            )
        )

        assertEquals(OneExteriorZoneKind.HOME, snapshot.currentZone?.kind)
        assertEquals("Home", snapshot.currentZone?.label)
    }
}
