package com.one.cognitivecompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class OneMedicationNotificationsTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun dailyRuleUsesTheNextTimeAndSupportsMultipleSlots() {
        val from = Instant.parse("2026-09-14T09:00:00Z")

        assertEquals(
            Instant.parse("2026-09-14T20:00:00Z"),
            OneMedicationScheduler.nextMedicationTime("08:00,20:00", from, zone)
        )
    }

    @Test
    fun weekdayRuleSkipsDaysOutsideItsConstraint() {
        val from = Instant.parse("2026-09-14T09:00:00Z") // Monday

        assertEquals(
            Instant.parse("2026-09-16T08:00:00Z"),
            OneMedicationScheduler.nextMedicationTime("Mon,Wed,Fri @ 08:00", from, zone)
        )
    }

    @Test
    fun weekdayAndWeekendAliasesAreSupported() {
        val from = Instant.parse("2026-09-18T09:00:00Z") // Friday

        assertEquals(
            Instant.parse("2026-09-19T08:00:00Z"),
            OneMedicationScheduler.nextMedicationTime("weekends 08:00", from, zone)
        )
    }

    @Test
    fun exactDateRuleDoesNotRepeatAfterItsDate() {
        val from = Instant.parse("2026-09-13T09:00:00Z")

        assertEquals(
            Instant.parse("2026-09-14T08:00:00Z"),
            OneMedicationScheduler.nextMedicationTime("2026-09-14 @ 08:00", from, zone)
        )
        assertNull(
            OneMedicationScheduler.nextMedicationTime(
                "2026-09-14 @ 08:00",
                Instant.parse("2026-09-14T09:00:00Z"),
                zone
            )
        )
    }

    @Test
    fun unsupportedRuleDoesNotCreateAnAlarm() {
        assertNull(
            OneMedicationScheduler.nextMedicationTime(
                "when needed",
                Instant.parse("2026-09-14T09:00:00Z"),
                zone
            )
        )
    }
}
