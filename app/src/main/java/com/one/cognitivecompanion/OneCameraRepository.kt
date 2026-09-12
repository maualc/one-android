package com.one.cognitivecompanion

import java.time.Instant
import java.util.UUID

data class OneCamera(
    val id: UUID,
    val name: String,
    val roomId: UUID?,
    val platform: String,
    val status: String,
    val enabled: Boolean,
    val lastSeenAt: Instant?
)

enum class OneCameraLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

interface OneCameraRepository {
    suspend fun load(session: OneSession): List<OneCamera>
}

/** Reads the consent-gated camera inventory without opening a live stream. */
class OneApiCameraRepository(
    private val apiClient: OneApiClient
) : OneCameraRepository {
    override suspend fun load(session: OneSession): List<OneCamera> = apiClient.homeCameras(session).map { camera ->
        OneCamera(
            id = camera.id,
            name = camera.name,
            roomId = camera.roomId,
            platform = camera.platform,
            status = camera.status,
            enabled = camera.enabled,
            lastSeenAt = camera.lastSeenAt
        )
    }
}
