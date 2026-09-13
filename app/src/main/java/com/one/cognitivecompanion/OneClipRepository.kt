package com.one.cognitivecompanion

import java.time.Instant
import java.util.UUID

data class OneClip(
    val id: UUID,
    val eventId: UUID,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val expiresAt: Instant?
)

enum class OneClipLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

interface OneClipRepository {
    suspend fun load(session: OneSession): List<OneClip>
}

/** Reads short-lived, consented event-clip metadata without downloading media. */
class OneApiClipRepository(
    private val apiClient: OneApiClient
) : OneClipRepository {
    override suspend fun load(session: OneSession): List<OneClip> = apiClient.homeClips(session).map { clip ->
        OneClip(
            id = clip.id,
            eventId = clip.eventId,
            startsAt = clip.startsAt,
            endsAt = clip.endsAt,
            expiresAt = clip.expiresAt
        )
    }
}
