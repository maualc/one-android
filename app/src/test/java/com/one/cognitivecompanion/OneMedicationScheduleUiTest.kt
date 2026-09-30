package com.one.cognitivecompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneMedicationScheduleUiTest {
    @Test fun dailyAndSelectedDaysAreParsed() {
        assertEquals(listOf("08:00", "20:00"), parseSimpleMedicationSchedule("08:00,20:00")?.times)
        assertEquals(setOf("Mon", "Wed", "Fri"), parseSimpleMedicationSchedule("Mon,Wed,Fri @ 08:00")?.days)
    }

    @Test fun customAndInvalidRulesAreNeverSilentlyRewritten() {
        assertNull(parseSimpleMedicationSchedule("weekdays 08:00"))
        assertNull(parseSimpleMedicationSchedule("morning"))
        assertNull(parseSimpleMedicationSchedule("26:00"))
    }
}
