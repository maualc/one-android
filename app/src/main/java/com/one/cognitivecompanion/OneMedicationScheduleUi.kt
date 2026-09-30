package com.one.cognitivecompanion

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalTime

private val scheduleDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val scheduleTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")

internal data class SimpleMedicationSchedule(val days: Set<String>, val times: List<String>)

internal fun parseSimpleMedicationSchedule(value: String): SimpleMedicationSchedule? {
    val sections = value.trim().split(" @ ", limit = 2)
    val dayNames = if (sections.size == 2) sections[0].split(",").map(String::trim) else emptyList()
    val timePart = sections.last()
    if (dayNames.any { it !in scheduleDays } || dayNames.distinct().size != dayNames.size) return null
    val times = timePart.split(",").map(String::trim)
    if (times.isEmpty() || times.any { !scheduleTime.matches(it) } || times.distinct().size != times.size) return null
    return SimpleMedicationSchedule(dayNames.toSet(), times)
}

private fun encodeSimpleMedicationSchedule(schedule: SimpleMedicationSchedule): String {
    val times = schedule.times.joinToString(",")
    val days = scheduleDays.filter { it in schedule.days }
    return if (days.isEmpty() || days.size == 7) times else "${days.joinToString(",")} @ $times"
}

@Composable
internal fun MedicationScheduleEditor(value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val parsed = parseSimpleMedicationSchedule(value)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Days and times", style = MaterialTheme.typography.titleMedium)
        if (parsed == null) {
            Text("Existing custom schedule. It will stay unchanged unless you edit it.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value, onChange, label = { Text("Schedule rule") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { onChange("08:00") }, modifier = Modifier.fillMaxWidth()) { Text("Use day and time picker") }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    FilterChip(selected = parsed.days.isEmpty(), onClick = { onChange(encodeSimpleMedicationSchedule(parsed.copy(days = emptySet()))) }, label = { Text("Daily") })
                }
                items(scheduleDays) { day ->
                    FilterChip(
                        selected = day in parsed.days,
                        onClick = {
                            val next = if (day in parsed.days) parsed.days - day else parsed.days + day
                            onChange(encodeSimpleMedicationSchedule(parsed.copy(days = next)))
                        },
                        label = { Text(day) }
                    )
                }
            }
            parsed.times.forEachIndexed { index, time ->
                OutlinedButton(onClick = {
                    val current = LocalTime.parse(time)
                    TimePickerDialog(context, { _, hour, minute ->
                        val changed = "%02d:%02d".format(hour, minute)
                        onChange(encodeSimpleMedicationSchedule(parsed.copy(times = parsed.times.toMutableList().also { it[index] = changed }.distinct())))
                    }, current.hour, current.minute, true).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Time ${index + 1} · $time") }
            }
            OutlinedButton(onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    val added = "%02d:%02d".format(hour, minute)
                    onChange(encodeSimpleMedicationSchedule(parsed.copy(times = (parsed.times + added).distinct())))
                }, 12, 0, true).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Add another time") }
            if (parsed.times.size > 1) OutlinedButton(
                onClick = { onChange(encodeSimpleMedicationSchedule(parsed.copy(times = parsed.times.dropLast(1)))) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Remove last time") }
            Text("Schedule: $value", style = MaterialTheme.typography.bodySmall)
        }
    }
}
