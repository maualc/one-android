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
}

