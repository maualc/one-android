package com.one.cognitivecompanion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OneMapUsabilityTest {
    private val polygon = OneMapPolygon(
        id = "room",
        label = "Living room",
        points = listOf(
            OneMapPoint(0f, 0f),
            OneMapPoint(4f, 0f),
            OneMapPoint(4f, 3f)
        ),
        confidence = 0.9f
    )

    private fun map(
        source: OneMapSource = OneMapSource.CAMERA_CV_2D,
        geometryStatus: String = "ready",
        rescanRequired: Boolean = false,
        polygons: List<OneMapPolygon> = listOf(polygon)
    ) = OneRoomMap(
        id = UUID.randomUUID(),
        homeId = UUID.randomUUID(),
        roomId = null,
        revision = 1,
        coordinateFrame = "image-space",
        zones = listOf("Living room"),
        createdAt = null,
        geometryStatus = geometryStatus,
        rescanRequired = rescanRequired,
        source = source,
        polygons = polygons
    )

    @Test
    fun validatedCameraGeometryIsUsable() {
        assertTrue(map().isUsableForCareTeam())
    }

    @Test
    fun legacyOrRejectedRevisionsAreNotUsable() {
        assertFalse(map(source = OneMapSource.LEGACY_2D).isUsableForCareTeam())
        assertFalse(map(geometryStatus = "rescan-required", rescanRequired = true).isUsableForCareTeam())
        assertFalse(map(polygons = emptyList()).isUsableForCareTeam())
    }
}
