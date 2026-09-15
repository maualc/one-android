package com.one.cognitivecompanion

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import org.json.JSONObject
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

data class OneScheduledMedication(
    val planId: UUID,
    val name: String,
    val dose: String,
    val instructions: String,
    val scheduleRule: String,
    val atEpochMillis: Long
)

/** Small AlarmManager adapter; it has no external scheduler dependency. */
object OneMedicationScheduler {
    private const val PREFS = "one.medication.alarms"
    private const val RECORDS = "records"
    const val ACTION_FIRE = "com.one.cognitivecompanion.action.MEDICATION_ALARM"
    const val CHANNEL_ID = "one_medication_reminders"

    private val timePattern = Regex("(?<!\\d)(?:[01]?\\d|2[0-3]):[0-5]\\d(?!\\d)")
    private val datePattern = Regex("20\\d{2}-\\d{2}-\\d{2}")
    private val dayAliases = mapOf(
        "mon" to DayOfWeek.MONDAY,
        "monday" to DayOfWeek.MONDAY,
        "tue" to DayOfWeek.TUESDAY,
        "tues" to DayOfWeek.TUESDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thu" to DayOfWeek.THURSDAY,
        "thur" to DayOfWeek.THURSDAY,
        "thurs" to DayOfWeek.THURSDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY,
        "friday" to DayOfWeek.FRIDAY,
        "sat" to DayOfWeek.SATURDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sun" to DayOfWeek.SUNDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    private data class ScheduleSlot(
        val exactDate: LocalDate?,
        val weekdays: Set<DayOfWeek>?,
        val time: LocalTime
    )

    fun schedule(context: Context, reminder: OneRemoteMedicationReminder): Boolean {
        // The backend returns today's reminders with their current check-in
        // status. Only pending doses should create a resident notification;
        // a taken, skipped or missed dose must not be resurrected locally.
        if (!reminder.status.equals("pending", ignoreCase = true)) return false
        val now = System.currentTimeMillis()
        val triggerAt = reminder.scheduledFor?.toEpochMilli()?.takeIf { it > now }
            ?: parseNextTime(reminder.scheduleRule)?.toEpochMilli()
            ?: return false
        val scheduled = OneScheduledMedication(
            planId = reminder.planId,
            name = reminder.name,
            dose = reminder.dose,
            instructions = reminder.instructions,
            scheduleRule = reminder.scheduleRule,
            atEpochMillis = triggerAt
        )
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val records = loadRecords(prefs).filterNot { it.planId == scheduled.planId && it.atEpochMillis == scheduled.atEpochMillis } + scheduled
        prefs.edit { putString(RECORDS, records.joinToString("\n") { encode(it) }) }
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context, scheduled)
        )
        return true
    }

    /**
     * Reconcile the alarms for the plans represented by a fresh API response.
     * A plan can change its time, become inactive, or return a different
     * occurrence, so simply adding new AlarmManager entries leaves stale
     * notifications behind. The response is authoritative for the plan IDs it
     * contains; failed API calls never reach this method and therefore retain
     * the last known schedule.
     */
    fun sync(context: Context, reminders: List<OneRemoteMedicationReminder>) {
        reminders.map { it.planId }.toSet().forEach { cancel(context, it) }
        reminders.forEach { schedule(context, it) }
    }

    fun cancel(context: Context, planId: UUID) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        loadRecords(prefs).filter { it.planId == planId }.forEach { record ->
            appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(appContext, record))
        }
        saveRecords(prefs, loadRecords(prefs).filterNot { it.planId == planId })
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alarm = appContext.getSystemService(AlarmManager::class.java)
        loadRecords(prefs).forEach { alarm.cancel(pendingIntent(appContext, it)) }
        saveRecords(prefs, emptyList())
    }

    internal fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val records = loadRecords(prefs).filter { it.atEpochMillis > now }
        saveRecords(prefs, records)
        val alarm = appContext.getSystemService(AlarmManager::class.java)
        records.forEach { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.atEpochMillis, pendingIntent(appContext, it)) }
    }

    internal fun fired(context: Context, planId: UUID, atEpochMillis: Long) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val records = loadRecords(prefs)
        val fired = records.firstOrNull { it.planId == planId && it.atEpochMillis == atEpochMillis }
        saveRecords(prefs, records.filterNot { it.planId == planId && it.atEpochMillis == atEpochMillis })
        if (fired != null && fired.scheduleRule.isNotBlank()) {
            parseNextTime(fired.scheduleRule, Instant.ofEpochMilli(atEpochMillis))?.let { next ->
                schedule(appContext, OneRemoteMedicationReminder(fired.planId, fired.name, fired.dose, fired.instructions, fired.scheduleRule, next, "pending", null, null))
            }
        }
    }

    /**
     * Resolve the next occurrence using the same small grammar as the backend:
     * daily times (08:00,20:00), weekday rules (Mon,Wed,Fri @ 08:00,
     * weekdays 08:00, weekends 08:00), and exact dates (2026-09-12 @ 08:00).
     * Unsupported text produces no occurrence instead of guessing.
     */
    internal fun nextMedicationTime(
        rule: String,
        from: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Instant? {
        val slots = parseSlots(rule)
        if (slots.isEmpty()) return null
        return slots.mapNotNull { slot ->
            if (slot.exactDate != null) {
                ZonedDateTime.of(slot.exactDate, slot.time, zone).toInstant()
                    .takeIf { it > from }
            } else {
                val startDate = from.atZone(zone).toLocalDate()
                (0..370).asSequence()
                    .map { offset -> startDate.plusDays(offset.toLong()) }
                    .filter { date -> slot.weekdays == null || date.dayOfWeek in slot.weekdays }
                    .map { date -> ZonedDateTime.of(date, slot.time, zone).toInstant() }
                    .firstOrNull { candidate -> candidate > from }
            }
        }.minOrNull()
    }

    private fun parseNextTime(rule: String, from: Instant = Instant.now()): Instant? =
        nextMedicationTime(rule, from)

    private fun parseSlots(rule: String): List<ScheduleSlot> = buildList {
        rule.split(';', '\n').forEach { rawSegment ->
            val segment = rawSegment.trim()
            if (segment.isBlank()) return@forEach
            val matches = timePattern.findAll(segment).toList()
            if (matches.isEmpty()) return@forEach
            val prefix = segment.substring(0, matches.first().range.first)
                .trim()
                .trim('@', ':', '-', ',')
                .lowercase()
            val dateText = datePattern.find(prefix)?.value
            val exactDate = dateText?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            }
            // A malformed date must not silently become a daily reminder.
            if (dateText != null && exactDate == null) return@forEach
            val weekdays = when {
                exactDate != null -> null
                prefix == "weekday" || prefix == "weekdays" ->
                    setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                prefix == "weekend" || prefix == "weekends" ->
                    setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                else -> {
                    val found = dayAliases.filterKeys { alias ->
                        Regex("(?<![a-z])${Regex.escape(alias)}(?![a-z])").containsMatchIn(prefix)
                    }.values.toSet()
                    found.ifEmpty { null }
                }
            }
            matches.forEach { match ->
                val time = runCatching {
                    val parts = match.value.split(':')
                    LocalTime.of(parts[0].toInt(), parts[1].toInt())
                }.getOrNull() ?: return@forEach
                add(ScheduleSlot(exactDate = exactDate, weekdays = weekdays, time = time))
            }
        }
    }

    private fun pendingIntent(context: Context, record: OneScheduledMedication): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(record),
            Intent(context, OneMedicationAlarmReceiver::class.java).apply {
                action = ACTION_FIRE
                putExtra(EXTRA_PLAN_ID, record.planId.toString())
                putExtra(EXTRA_NAME, record.name)
                putExtra(EXTRA_DOSE, record.dose)
                putExtra(EXTRA_INSTRUCTIONS, record.instructions)
                putExtra(EXTRA_AT, record.atEpochMillis)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun requestCode(record: OneScheduledMedication): Int = ("${record.planId}:" + record.atEpochMillis).hashCode()

    private fun loadRecords(prefs: android.content.SharedPreferences): List<OneScheduledMedication> = prefs.getString(RECORDS, null).orEmpty()
        .lineSequence().mapNotNull { decode(it) }.toList()

    private fun saveRecords(prefs: android.content.SharedPreferences, records: List<OneScheduledMedication>) {
        prefs.edit { putString(RECORDS, records.joinToString("\n") { encode(it) }) }
    }

    private fun encode(record: OneScheduledMedication): String = JSONObject()
        .put("plan_id", record.planId.toString())
        .put("name", record.name)
        .put("dose", record.dose)
        .put("instructions", record.instructions)
        .put("schedule_rule", record.scheduleRule)
        .put("at", record.atEpochMillis)
        .toString()

    private fun decode(raw: String): OneScheduledMedication? = runCatching {
        val body = JSONObject(raw)
        OneScheduledMedication(
            planId = UUID.fromString(body.getString("plan_id")),
            name = body.optString("name"),
            dose = body.optString("dose"),
            instructions = body.optString("instructions"),
            scheduleRule = body.optString("schedule_rule"),
            atEpochMillis = body.getLong("at")
        )
    }.getOrNull()

    private const val EXTRA_PLAN_ID = "plan_id"
    private const val EXTRA_NAME = "name"
    private const val EXTRA_DOSE = "dose"
    private const val EXTRA_INSTRUCTIONS = "instructions"
    private const val EXTRA_AT = "at"
}

class OneMedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != OneMedicationScheduler.ACTION_FIRE) return
        val planId = intent.getStringExtra("plan_id")?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return
        val at = intent.getLongExtra("at", 0L)
        OneMedicationScheduler.fired(context, planId, at)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(OneMedicationScheduler.CHANNEL_ID, context.getString(R.string.one_medication_channel), NotificationManager.IMPORTANCE_HIGH))
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val name = intent.getStringExtra("name").orEmpty().ifBlank { context.getString(R.string.one_medication_notification_name) }
        val dose = intent.getStringExtra("dose").orEmpty()
        val instructions = intent.getStringExtra("instructions").orEmpty()
        manager.notify(
            ("medication:$planId:$at").hashCode(),
            NotificationCompat.Builder(context, OneMedicationScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("ONE · $name")
                .setContentText(listOf(dose, instructions).filter { it.isNotBlank() }.joinToString(" · "))
                .setStyle(NotificationCompat.BigTextStyle().bigText(instructions.ifBlank { context.getString(R.string.one_medication_notification_fallback) }))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }
}

class OneBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            OneMedicationScheduler.rescheduleAll(context)
        }
    }
}
