package com.one.cognitivecompanion

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class OneMedicationLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class OneMedicationPlanActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

interface OneMedicationRepository {
    suspend fun load(session: OneSession, subjectUserId: UUID? = null, careRecipientId: UUID? = null): List<MedicationDose>
}

/** Reads today's deterministic, consent-gated reminder list. */
class OneApiMedicationRepository(
    private val apiClient: OneApiClient
) : OneMedicationRepository {
    override suspend fun load(session: OneSession, subjectUserId: UUID?, careRecipientId: UUID?): List<MedicationDose> = apiClient.medicationReminders(
        session,
        subjectUserId = subjectUserId,
        careRecipientId = careRecipientId
    ).map { reminder ->
        MedicationDose(
            name = reminder.name,
            instructions = listOf(reminder.dose, reminder.instructions)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { "No instructions provided" },
            time = reminder.scheduledFor?.toLocalTimeLabel() ?: "Unscheduled",
            status = reminder.status.toDoseStatus(),
            assignedTo = reminder.assignedCaregiverName,
            planId = reminder.planId,
            scheduledFor = reminder.scheduledFor
        )
    }
}

private fun String.toDoseStatus(): DoseStatus = when (lowercase()) {
    "taken" -> DoseStatus.TAKEN
    "skipped" -> DoseStatus.SKIPPED
    "missed" -> DoseStatus.MISSED
    "scheduled" -> DoseStatus.SCHEDULED
    else -> DoseStatus.PENDING
}

private fun Instant.toLocalTimeLabel(): String = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(this)
