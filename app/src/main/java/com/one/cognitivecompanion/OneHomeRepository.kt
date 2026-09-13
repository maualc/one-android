package com.one.cognitivecompanion

/** Domain snapshot used by the caregiver home screen. */
data class OneHomeSnapshot(
    val profile: OneHomeProfile,
    val objects: List<OneRemoteObject>,
    val events: List<OneEvent>
)

enum class OneHomeLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

interface OneHomeRepository {
    suspend fun load(session: OneSession): OneHomeSnapshot
}

/** Reads the three read-only dashboard resources from the versioned API. */
class OneApiHomeRepository(
    private val apiClient: OneApiClient
) : OneHomeRepository {
    override suspend fun load(session: OneSession): OneHomeSnapshot {
        val profile = apiClient.homeProfile(session)
        val objects = apiClient.homeObjects(session)
        val events = apiClient.homeEvents(session).map(OneRemoteEvent::toOneEvent)
        return OneHomeSnapshot(profile = profile, objects = objects, events = events)
    }
}

private fun OneRemoteEvent.toOneEvent(): OneEvent = OneEvent(
    kind = when (type.lowercase()) {
        "check_in", "check-in", "checkin" -> EventKind.CHECK_IN
        "assistant", "assistant_request" -> EventKind.ASSISTANT
        else -> EventKind.MOVEMENT
    },
    location = "Home · approximate",
    time = lastSeenAt?.toString() ?: "Time unavailable",
    explanation = explanation,
    confidence = when {
        confidence >= 0.8 -> "High confidence"
        confidence >= 0.5 -> "Medium confidence"
        else -> "Low confidence"
    },
    id = id,
    observedAt = lastSeenAt
)
