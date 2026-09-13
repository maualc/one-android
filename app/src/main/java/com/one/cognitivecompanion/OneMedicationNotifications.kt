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
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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

    fun schedule(context: Context, reminder: OneRemoteMedicationReminder): Boolean {
        val triggerAt = reminder.scheduledFor?.toEpochMilli()
            ?: parseNextTime(reminder.scheduleRule)?.toEpochMilli()
            ?: return false
        if (triggerAt <= System.currentTimeMillis()) return false
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
        prefs.edit().putString(RECORDS, records.joinToString("\n") { encode(it) }).apply()
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context, scheduled)
        )
        return true
    }

    fun cancel(context: Context, planId: UUID) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        loadRecords(prefs).filter { it.planId == planId }.forEach { record ->
            appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(appContext, record))
        }
        saveRecords(prefs, loadRecords(prefs).filterNot { it.planId == planId })
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

    private fun parseNextTime(rule: String, from: Instant = Instant.now()): Instant? {
        val match = Regex("\\b([01]?[0-9]|2[0-3]):([0-5][0-9])\\b").find(rule) ?: return null
        val time = LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        val zone = ZoneId.systemDefault()
        var date = LocalDate.now(zone)
        var local = LocalDateTime.of(date, time)
        if (local.atZone(zone).toInstant() <= from) {
            date = date.plusDays(1)
            local = LocalDateTime.of(date, time)
        }
        return local.atZone(zone).toInstant()
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
        prefs.edit().putString(RECORDS, records.joinToString("\n") { encode(it) }).apply()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(OneMedicationScheduler.CHANNEL_ID, context.getString(R.string.one_medication_channel), NotificationManager.IMPORTANCE_HIGH))
        }
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val name = intent.getStringExtra("name").orEmpty()
        val dose = intent.getStringExtra("dose").orEmpty()
        val instructions = intent.getStringExtra("instructions").orEmpty()
        manager.notify(
            ("medication:$planId:$at").hashCode(),
            NotificationCompat.Builder(context, OneMedicationScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("ONE · $name")
                .setContentText(listOf(dose, instructions).filter { it.isNotBlank() }.joinToString(" · "))
                .setStyle(NotificationCompat.BigTextStyle().bigText(instructions.ifBlank { "Es hora de revisar esta medicación." }))
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
