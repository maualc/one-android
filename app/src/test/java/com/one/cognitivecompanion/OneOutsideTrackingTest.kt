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
