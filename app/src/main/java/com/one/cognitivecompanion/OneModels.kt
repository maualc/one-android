package com.one.cognitivecompanion

import java.time.Instant
import java.util.UUID

/** Product experiences exposed by ONE. Publisher is a device-only role. */
enum class OneRole {
    CAREGIVER,
    RESIDENT,
    PUBLISHER
}

enum class AuthStage {
    SIGNED_OUT,
    ONBOARDING,
    AUTHENTICATED
}

enum class OneAssistantLoadState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneFamilyAssistantLoadState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneConsentLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class OneExportLoadState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneDeletionLoadState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneBackendHealthLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class OneFamilyInviteLoadState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneCareSpaceLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class OneCareSpaceActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneCareRecipientLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class OneCareRecipientActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneFamilyMemberActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneCameraPairingStatusLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

enum class OneCameraReconnectLoadState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneMapLoadState {
    IDLE,
    LOADING,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneCalibrationActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneObjectActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

enum class OneObservationActionState {
    IDLE,
    SUBMITTING,
    LOADED,
    ERROR
}

/**
 * Provenance exposed by the backend map contract.  Android currently renders
 * the 2D paths only; keeping the source explicit prevents a camera map from
 * being presented as a metric 3D model by accident.
 */
enum class OneMapSource(val wireValue: String) {
    CAMERA_CV_2D("camera-cv-2d"),
    ROOMPLAN_LIDAR_3D("roomplan-lidar-3d"),
    ARKIT_VIDEO_3D("arkit-video-3d"),
    LEGACY_2D("legacy-2d"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): OneMapSource = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class OneMapDimension(val wireValue: String) {
    TWO_D("2d"),
    THREE_D("3d"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): OneMapDimension = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

data class OneMapPoint(
    val x: Float,
    val y: Float
)

data class OneMapPolygon(
    val id: String,
    val label: String,
    val points: List<OneMapPoint>,
    val confidence: Float?
)

data class OneMapWall(
    val id: String,
    val start: OneMapPoint,
    val end: OneMapPoint,
    val confidence: Float?
)

data class OneMapFurniture(
    val id: String,
    val label: String,
    val center: OneMapPoint,
    val size: OneMapPoint,
    val rotationDegrees: Float,
    val confidence: Float?
)

data class OneMapOpening(
    val id: String,
    val kind: String,
    val start: OneMapPoint,
    val end: OneMapPoint,
    val confidence: Float?
)

enum class DoseStatus(val label: String) {
    ACKNOWLEDGED("Acknowledged"),
    NEEDS_CONFIRMATION("Needs confirmation"),
    SCHEDULED("Scheduled"),
    PENDING("Pending"),
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    MISSED("Missed")
}

data class MedicationDose(
    val name: String,
    val instructions: String,
    val time: String,
    val status: DoseStatus,
    val assignedTo: String? = null,
    val planId: UUID? = null,
    val scheduledFor: Instant? = null
)

enum class EventKind(val label: String) {
    CHECK_IN("Daily check-in"),
    MOVEMENT("Movement observed"),
    ASSISTANT("Assistant request"),
    OBJECT_OBSERVED("Object observed"),
    OTHER("Observed activity")
}

data class OneEvent(
    val kind: EventKind,
    val location: String,
    val time: String,
    val explanation: String,
    val confidence: String,
    val id: UUID? = null,
    val observedAt: Instant? = null,
    val evidenceIds: List<String> = emptyList()
)

data class ConsentChoice(
    val label: String,
    val enabled: Boolean
)

val demoMedicationDoses = listOf(
    MedicationDose("Morning reminder", "With breakfast", "08:30", DoseStatus.ACKNOWLEDGED, "Marta Martínez"),
    MedicationDose("Midday reminder", "After lunch", "13:00", DoseStatus.NEEDS_CONFIRMATION, "Joan Soler"),
    MedicationDose("Evening reminder", "With dinner", "20:00", DoseStatus.SCHEDULED)
)

val demoEvents = listOf(
    OneEvent(EventKind.CHECK_IN, "Living room", "Today · 08:42", "A familiar morning check-in was completed.", "High confidence"),
    OneEvent(EventKind.MOVEMENT, "Kitchen · approximate", "Yesterday · 12:18", "Movement was observed near the calibrated kitchen zone.", "Medium confidence"),
    OneEvent(EventKind.ASSISTANT, "Home", "Mon · 10:05", "The resident used push-to-talk to ask for the day's reminder.", "High confidence")
)

val defaultConsentChoices = listOf(
    ConsentChoice("Room scan and map", true),
    ConsentChoice("Microphone for push-to-talk", true),
    ConsentChoice("Caregiver event clips", false)
)
