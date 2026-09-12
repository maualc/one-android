package com.one.cognitivecompanion

/** Domain snapshot used by the caregiver home screen. */
data class OneHomeSnapshot(
    val profile: OneHomeProfile,
    val objects: List<OneRemoteObject>,
    val events: List<OneRemoteEvent>
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
        val events = apiClient.homeEvents(session)
        return OneHomeSnapshot(profile = profile, objects = objects, events = events)
    }
}
