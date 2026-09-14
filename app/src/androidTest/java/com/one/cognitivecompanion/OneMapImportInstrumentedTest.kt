package com.one.cognitivecompanion

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OneMapImportInstrumentedTest {
    @Test
    fun parsesPortableStringAndObjectZones() {
        val result = parseOneMapImport(
            """{"room_name":"Kitchen","coordinate_frame":"arcore-local","zones":["counter",{"label":"entry"},"counter"]}"""
        )
        assertEquals("Kitchen", result.roomName)
        assertEquals(listOf("counter", "entry"), result.zones)
        assertEquals("arcore-local", result.coordinateFrame)
    }

    @Test
    fun rejectsMapsWithoutZones() {
        assertThrows(IllegalArgumentException::class.java) { parseOneMapImport("{\"room_name\":\"Empty\"}") }
    }

    @Test
    fun readsBackendGeometryWithoutPromotingItToThreeD() {
        val result = parseOneMapImport(
            """{"source":"camera-cv-2d","dimension":"2d","coordinate_frame":"camera-relative-image","map_data":{"geometry":{"polygons":[{"label":"Living room","points":[{"x":0,"y":0},{"x":1,"y":0},{"x":1,"y":1}]}]},"zones":[{"label":"Living room"}]}}"""
        )
        assertEquals(OneMapSource.CAMERA_CV_2D, result.source)
        assertEquals(OneMapDimension.TWO_D, result.dimension)
        assertEquals(1, result.geometryItemCount)
        assertEquals(false, result.rescanRequired)
    }

    @Test
    fun marksLegacyDataForRescan() {
        val result = parseOneMapImport("""{"zones":["Entry"],"source":"legacy-2d"}""")
        assertEquals(OneMapSource.LEGACY_2D, result.source)
        assertEquals(true, result.rescanRequired)
    }
}
