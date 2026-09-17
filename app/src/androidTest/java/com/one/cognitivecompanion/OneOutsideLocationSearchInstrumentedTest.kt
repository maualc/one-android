package com.one.cognitivecompanion

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OneOutsideLocationSearchInstrumentedTest {
    @Test
    fun parsesSearchResultsAndSkipsIncompleteEntries() {
        val results = parseOneOutsideLocationSearchResults(
            """
            [
              {"display_name":"Puerta del Sol, Madrid","lat":"40.4168","lon":"-3.7038"},
              {"display_name":"Incomplete result","lat":"40.4"},
              {"display_name":"Parc Güell, Barcelona","lat":"41.4145","lon":"2.1527"}
            ]
            """.trimIndent()
        )

        assertEquals(2, results.size)
        assertEquals("Puerta del Sol, Madrid", results.first().displayName)
        assertEquals(40.4168, results.first().point.latitude, 0.000001)
        assertEquals(-3.7038, results.first().point.longitude, 0.000001)
        assertTrue(results.last().displayName.contains("Barcelona"))
    }
}
