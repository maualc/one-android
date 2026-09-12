package com.one.cognitivecompanion

import java.util.UUID

data class OneFamilyMember(
    val id: UUID,
    val displayName: String,
    val email: String?,
    val role: String,
    val representationStatus: String?
)

enum class OneFamilyLoadState {
    IDLE,
    LOADING,
    LOADED,
    ERROR
}

interface OneFamilyRepository {
    suspend fun load(session: OneSession): List<OneFamilyMember>
}

/** Reads the consent-gated care-circle membership endpoint. */
class OneApiFamilyRepository(
    private val apiClient: OneApiClient
) : OneFamilyRepository {
    override suspend fun load(session: OneSession): List<OneFamilyMember> = apiClient.familyMembers(session).map { member ->
        OneFamilyMember(
            id = member.id,
            displayName = member.displayName,
            email = member.email,
            role = member.role,
            representationStatus = member.representationStatus
        )
    }
}
