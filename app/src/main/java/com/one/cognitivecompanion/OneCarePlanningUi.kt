package com.one.cognitivecompanion

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun CarePlanningScreen(
    session: OneSession?,
    selectedRecipientId: UUID?,
    selectedRecipientName: String?,
    consented: Boolean,
    entries: List<OneCareEntry>?,
    loadError: String?,
    actionError: String?,
    saving: Boolean,
    onEnableConsent: (UUID) -> Unit,
    onRefresh: () -> Unit,
    onSave: suspend (OneCareEntryRequest, OneCareEntry?) -> Boolean,
    onRemove: suspend (OneCareEntry) -> Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drafts = remember(context) { OneSecureStore(context) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf("note") }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var time by rememberSaveable { mutableStateOf("09:00") }
    var endEnabled by rememberSaveable { mutableStateOf(false) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var endTime by rememberSaveable { mutableStateOf("10:00") }
    var reminderMinutes by rememberSaveable { mutableStateOf<Int?>(30) }
    var editing by remember { mutableStateOf<OneCareEntry?>(null) }
    var removeTarget by remember { mutableStateOf<OneCareEntry?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val zone = remember { ZoneId.systemDefault() }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun openEditor(nextKind: String, entry: OneCareEntry? = null) {
        if (session == null || selectedRecipientId == null) return
        editing = entry
        kind = entry?.kind ?: nextKind
        val formKey = entry?.let { "$kind:${it.id}" } ?: kind
        val draft = drafts.readDraft(session, selectedRecipientId, formKey)?.let { runCatching { JSONObject(it) }.getOrNull() }
        title = draft?.optString("title") ?: entry?.title.orEmpty()
        body = draft?.optString("body") ?: entry?.body.orEmpty()
        location = draft?.optString("location") ?: entry?.location.orEmpty()
        val localStart = entry?.startsAt?.atZone(zone)
        date = draft?.optString("date")?.takeIf(String::isNotBlank) ?: localStart?.toLocalDate()?.toString() ?: LocalDate.now().plusDays(1).toString()
        time = draft?.optString("time")?.takeIf(String::isNotBlank) ?: localStart?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "09:00"
        endEnabled = if (draft?.has("end_enabled") == true) draft.optBoolean("end_enabled") else entry?.endsAt != null
        endDate = draft?.optString("end_date")?.takeIf(String::isNotBlank) ?: entry?.endsAt?.atZone(zone)?.toLocalDate()?.toString() ?: date
        endTime = draft?.optString("end_time")?.takeIf(String::isNotBlank) ?: entry?.endsAt?.atZone(zone)?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "10:00"
        reminderMinutes = if (draft?.has("reminder") == true) draft.optInt("reminder").takeIf { it >= 0 } else entry?.reminderMinutes ?: 30
        localError = null
        editorOpen = true
    }

    LaunchedEffect(editorOpen, editing, kind, title, body, location, date, time, endEnabled, endDate, endTime, reminderMinutes) {
        if (editorOpen && session != null && selectedRecipientId != null) {
            val formKey = editing?.let { "$kind:${it.id}" } ?: kind
            drafts.saveDraft(session, selectedRecipientId, formKey, JSONObject()
                .put("title", title).put("body", body).put("location", location)
                .put("date", date).put("time", time).put("end_enabled", endEnabled)
                .put("end_date", endDate).put("end_time", endTime).put("reminder", reminderMinutes ?: -1).toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onClose) { Text("← Back") }
        Text("Notes and appointments", style = MaterialTheme.typography.headlineMedium)
        Text(selectedRecipientName ?: "Select a person in Family", style = MaterialTheme.typography.bodyMedium)
        when {
            session == null -> Text("Sign in to view shared care planning.")
            selectedRecipientId == null -> Text("Select a cared-for person in Family first.")
            !consented -> {
                Text("Sharing is off for this person. Consent is needed before notes or appointments can be stored.")
                Button(onClick = { onEnableConsent(selectedRecipientId) }) { Text("Enable sharing") }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openEditor("note") }, modifier = Modifier.weight(1f)) { Text("Add note") }
                    OutlinedButton(onClick = { openEditor("appointment") }, modifier = Modifier.weight(1f)) { Text("Add appointment") }
                }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
                loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (entries == null && loadError == null) Text("Loading…")
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val upcoming = entries.orEmpty().filter { it.kind == "appointment" && it.startsAt?.isAfter(Instant.now()) == true }.sortedBy { it.startsAt }
                    val notes = entries.orEmpty().filter { it.kind == "note" }.sortedByDescending { it.createdAt }
                    if (entries?.isEmpty() == true) Text("No notes or appointments yet.")
                    if (upcoming.isNotEmpty()) Text("Upcoming", style = MaterialTheme.typography.titleMedium)
                    upcoming.forEach { entry ->
                        CareEntryCard(entry, zone, session, { openEditor(entry.kind, entry) }, { removeTarget = entry })
                    }
                    if (notes.isNotEmpty()) Text("Notes", style = MaterialTheme.typography.titleMedium)
                    notes.forEach { entry -> CareEntryCard(entry, zone, session, { openEditor(entry.kind, entry) }, { removeTarget = entry }) }
                    val past = entries.orEmpty().filter { it.kind == "appointment" && it.startsAt?.isAfter(Instant.now()) != true }.sortedByDescending { it.startsAt }
                    if (past.isNotEmpty()) Text("Past appointments", style = MaterialTheme.typography.titleMedium)
                    past.forEach { entry -> CareEntryCard(entry, zone, session, { openEditor(entry.kind, entry) }, { removeTarget = entry }) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (editorOpen && session != null && selectedRecipientId != null) {
        Dialog(onDismissRequest = { if (!saving) editorOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize().imePadding().navigationBarsPadding(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (editing == null) "Add ${if (kind == "note") "note" else "appointment"}" else "Edit ${if (kind == "note") "note" else "appointment"}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editorOpen = false }, enabled = !saving) { Text("Close") }
                    }
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(body, { body = it }, label = { Text(if (kind == "note") "Note" else "Details (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        if (kind == "appointment") {
                            OutlinedTextField(location, { location = it }, label = { Text("Place (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = {
                                val selected = LocalDate.parse(date)
                                DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Date · $date") }
                            OutlinedButton(onClick = {
                                val selected = LocalTime.parse(time)
                                TimePickerDialog(context, { _, hour, minute -> time = "%02d:%02d".format(hour, minute) }, selected.hour, selected.minute, true).show()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Starts · $time") }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Set end time", modifier = Modifier.weight(1f))
                                Switch(checked = endEnabled, onCheckedChange = { endEnabled = it })
                            }
                            if (endEnabled) {
                                OutlinedButton(onClick = {
                                    val selected = LocalDate.parse(endDate)
                                    DatePickerDialog(context, { _, year, month, day -> endDate = LocalDate.of(year, month + 1, day).toString() }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
                                }, modifier = Modifier.fillMaxWidth()) { Text("End date · $endDate") }
                                OutlinedButton(onClick = {
                                    val selected = LocalTime.parse(endTime)
                                    TimePickerDialog(context, { _, hour, minute -> endTime = "%02d:%02d".format(hour, minute) }, selected.hour, selected.minute, true).show()
                                }, modifier = Modifier.fillMaxWidth()) { Text("Ends · $endTime") }
                            }
                            Text("Remind me", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(listOf(null to "Off", 5 to "5 min", 30 to "30 min", 1440 to "1 day")) { (minutes, label) ->
                                    FilterChip(selected = reminderMinutes == minutes, onClick = { reminderMinutes = minutes }, label = { Text(label) })
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && reminderMinutes != null && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                OutlinedButton(onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow appointment notifications") }
                            }
                        }
                        Text("For ${selectedRecipientName ?: "selected person"}", style = MaterialTheme.typography.bodySmall)
                        (localError ?: actionError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    HorizontalDivider()
                    Button(onClick = {
                        val starts = if (kind == "appointment") LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time)).atZone(zone).toInstant() else null
                        val ends = if (kind == "appointment" && endEnabled) LocalDateTime.of(LocalDate.parse(endDate), LocalTime.parse(endTime)).atZone(zone).toInstant() else null
                        if (title.isBlank()) {
                            localError = "Add a title."
                        } else if (kind == "note" && body.isBlank()) {
                            localError = "Write the note before saving."
                        } else if (ends != null && starts != null && !ends.isAfter(starts)) {
                            localError = "End time must be after start time."
                        } else {
                            localError = null
                            scope.launch {
                                val request = OneCareEntryRequest(selectedRecipientId, kind, title.trim(), body.trim(), if (kind == "appointment") location.trim() else "", starts, ends, if (kind == "appointment") zone.id else null, if (kind == "appointment") reminderMinutes else null)
                                if (onSave(request, editing)) {
                                    drafts.clearDraft(session, selectedRecipientId, editing?.let { "$kind:${it.id}" } ?: kind)
                                    editorOpen = false
                                }
                            }
                        }
                    }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving…" else "Save") }
                }
            }
        }
    }

    removeTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { if (!saving) removeTarget = null },
            title = { Text("Remove ${if (entry.kind == "note") "note" else "appointment"}?") },
            text = { Text("This removes the shared entry and cancels its local reminder after sync.") },
            confirmButton = {
                Button(onClick = { scope.launch { if (onRemove(entry)) removeTarget = null } }, enabled = !saving) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }, enabled = !saving) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CareEntryCard(entry: OneCareEntry, zone: ZoneId, session: OneSession, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            if (entry.kind == "appointment") {
                Text(entry.startsAt?.atZone(zone)?.format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")) ?: "Time unavailable")
                if (entry.location.isNotBlank()) Text(entry.location)
            }
            if (entry.body.isNotBlank()) Text(entry.body, style = MaterialTheme.typography.bodyMedium)
            if (entry.createdBy == session.userId || session.backendRole == "admin") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}
