package com.one.cognitivecompanion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OneOutsideTrackingPersistenceInstrumentedTest {
    @Test
    fun reliableAndUnreliableSamplesKeepHistoryAndAlertStateSeparate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = OneOutsideTrackingStore(context)
        val personId = UUID.randomUUID()
        val homePoint = OneExteriorPoint(40.4168, -3.7038)
        val outsidePoint = homePoint.offsetMeters(eastMeters = 400.0, northMeters = 0.0)
        val baseTime = System.currentTimeMillis() - ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS - 60_000L

        try {
            store.saveHome(personId, homePoint, radiusMeters = 10.0)
            val homeSnapshot = store.read(personId)
            assertEquals(ONE_OUTSIDE_MIN_ZONE_RADIUS_METERS, homeSnapshot.home?.radiusMeters ?: 0.0, 0.0)

            val first = store.appendLocation(
                personId = personId,
                point = homePoint,
                accuracyMeters = 8f,
                capturedAtMillis = baseTime,
                source = OneOutsideLocationSource.GPS
            )
            assertTrue(first.classified)
            assertTrue(first.alerts.isEmpty())

            val inaccurate = store.appendLocation(
                personId = personId,
                point = outsidePoint,
                accuracyMeters = 250f,
                capturedAtMillis = baseTime + 1_000L,
                source = OneOutsideLocationSource.GPS
            )
            assertFalse(inaccurate.classified)
            assertNull(inaccurate.point.zoneKey)
            assertTrue(inaccurate.alerts.isEmpty())

            val reliableOutside = store.appendLocation(
                personId = personId,
                point = outsidePoint,
                accuracyMeters = 8f,
                capturedAtMillis = baseTime + 2_000L,
                source = OneOutsideLocationSource.SIMULATED
            )
            assertEquals(listOf(OneOutsideAlertType.EXIT_HOME), reliableOutside.alerts.map { it.type })

            val stationaryPoint = outsidePoint.offsetMeters(eastMeters = 220.0, northMeters = 0.0)
            store.appendLocation(
                personId = personId,
                point = stationaryPoint,
                accuracyMeters = 8f,
                capturedAtMillis = baseTime + 10_000L,
                source = OneOutsideLocationSource.GPS
            )
            val stayed = store.appendLocation(
                personId = personId,
                point = stationaryPoint,
                accuracyMeters = 8f,
                capturedAtMillis = baseTime + 10_000L + ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS,
                source = OneOutsideLocationSource.GPS
            )
            assertTrue(stayed.point.dwellDurationMillis >= ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS)
            store.updateLocationStreetName(
                personId = personId,
                pointId = stayed.point.id,
                point = stayed.point.point,
                capturedAtMillis = stayed.point.capturedAtMillis,
                dwellDurationMillis = stayed.point.dwellDurationMillis,
                streetName = "Carrer de prova"
            )

            val persisted = store.read(personId)
            assertEquals(5, persisted.points.size)
            assertEquals(1, persisted.alerts.size)
            assertEquals("outside", persisted.lastZoneKey)
            assertTrue(
                persisted.points
                    .filter { it.point == stationaryPoint }
                    .all { it.streetName == "Carrer de prova" }
            )
        } finally {
            store.deletePerson(personId)
        }
    }

    @Test
    fun uploadedPointIsNotDuplicatedWhenItsRemoteCopyArrives() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = OneOutsideTrackingStore(context)
        val personId = UUID.randomUUID()
        try {
            val sample = store.appendLocation(personId, OneExteriorPoint(40.4168, -3.7038), 8f,
                System.currentTimeMillis(), OneOutsideLocationSource.GPS).point
            assertEquals(listOf(sample.id), store.pendingPoints(personId).map { it.id })
            store.markPointsUploaded(listOf(sample))
            assertTrue(store.pendingPoints(personId).isEmpty())
            store.upsertRemoteLocation(personId, UUID.randomUUID(), sample.point, 8f,
                sample.capturedAtMillis, clientSampleId = sample.id, streetName = "Calle Mayor")
            assertEquals(1, store.read(personId).points.size)
            assertEquals("Calle Mayor", store.read(personId).points.single().streetName)
            val remoteId = UUID.randomUUID()
            store.upsertRemoteLocation(personId, remoteId, sample.point, 8f, sample.capturedAtMillis)
            store.upsertRemoteLocation(personId, remoteId, sample.point, 8f, sample.capturedAtMillis,
                streetName = "Calle Nueva", dwellDurationMillis = ONE_OUTSIDE_DWELL_THRESHOLD_MILLIS)
            assertEquals(2, store.read(personId).points.size)
            assertEquals("Calle Nueva", store.read(personId).points.first { it.id == "remote:$remoteId" }.streetName)
        } finally {
            store.deletePerson(personId)
        }
    }
}
