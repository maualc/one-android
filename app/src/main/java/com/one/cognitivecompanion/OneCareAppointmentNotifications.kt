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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A synced appointment is a local reminder, never a server push guarantee. */
object OneCareAppointmentScheduler {
    private const val PREFS = "one.care.appointments"
    private const val RECORDS = "records"
    private const val OWNER = "owner"
    const val ACTION_FIRE = "com.one.cognitivecompanion.action.CARE_APPOINTMENT"
    const val CHANNEL_ID = "one_care_appointments"

    private data class Record(val id: UUID, val recipientId: UUID, val triggerAt: Long)

    fun bindSession(context: Context, session: OneSession) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = "${session.homeId}:${session.userId}"
        val previous = preferences.getString(OWNER, null)
        if (previous != null && previous != owner) clear(context)
        preferences.edit { putString(OWNER, owner) }
    }

    fun sync(context: Context, recipientId: UUID, entries: List<OneCareEntry>) {
        val old = records(context)
        old.filter { it.recipientId == recipientId }.forEach { cancel(context, it) }
        val now = System.currentTimeMillis()
        val upcoming = entries.mapNotNull { entry ->
            if (entry.kind != "appointment") return@mapNotNull null
            val startsAt = entry.startsAt ?: return@mapNotNull null
            val minutes = entry.reminderMinutes ?: return@mapNotNull null
            val triggerAt = startsAt.toEpochMilli() - minutes * 60_000L
            if (triggerAt <= now) null else Record(entry.id, recipientId, triggerAt)
        }
        val next = old.filterNot { it.recipientId == recipientId } + upcoming
        persist(context, next)
        upcoming.forEach { schedule(context, it) }
    }

    fun clear(context: Context) {
        records(context).forEach { cancel(context, it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(RECORDS); remove(OWNER) }
    }

    fun rescheduleAll(context: Context) {
        val session = runCatching { OneSecureStore(context).restore()?.session }.getOrNull()
        val owner = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(OWNER, null)
        if (session == null || owner != "${session.homeId}:${session.userId}") {
            clear(context)
            return
        }
        records(context).filter { it.triggerAt > System.currentTimeMillis() }.forEach { schedule(context, it) }
    }

    fun isCurrent(context: Context, id: UUID, triggerAt: Long): Boolean =
        records(context).any { it.id == id && it.triggerAt == triggerAt }

    private fun schedule(context: Context, record: Record) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, record.triggerAt, pendingIntent(context, record)
        )
    }

    private fun cancel(context: Context, record: Record) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, record))
    }

    private fun pendingIntent(context: Context, record: Record): PendingIntent = PendingIntent.getBroadcast(
        context, record.id.hashCode(), Intent(context, OneCareAppointmentAlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra("id", record.id.toString())
            .putExtra("trigger_at", record.triggerAt),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun records(context: Context): List<Record> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECORDS, "[]") ?: "[]"
        return runCatching {
            val rows = JSONArray(raw)
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    add(Record(UUID.fromString(row.getString("id")), UUID.fromString(row.getString("recipient")), row.getLong("trigger")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(context: Context, records: List<Record>) {
        val rows = JSONArray()
        records.forEach { rows.put(JSONObject().put("id", it.id.toString()).put("recipient", it.recipientId.toString()).put("trigger", it.triggerAt)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(RECORDS, rows.toString()) }
    }
}

class OneCareAppointmentAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != OneCareAppointmentScheduler.ACTION_FIRE) return
        val id = intent.getStringExtra("id")?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return
        val triggerAt = intent.getLongExtra("trigger_at", 0L)
        if (!OneCareAppointmentScheduler.isCurrent(context, id, triggerAt)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(OneCareAppointmentScheduler.CHANNEL_ID, "Care appointments", NotificationManager.IMPORTANCE_DEFAULT))
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id.hashCode(), NotificationCompat.Builder(context, OneCareAppointmentScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ONE · Care appointment")
            .setContentText("Upcoming appointment")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build())
    }
}
