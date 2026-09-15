package com.one.cognitivecompanion

/**
 * A map revision is only active in the care-team UI when its geometry has
 * passed the backend validation and there is something the caregiver can
 * actually inspect. Legacy/provisional rows are kept by the backend for
 * history, but must not be presented as the current usable map.
 */
internal fun OneRoomMap.isUsableForCareTeam(): Boolean {
    if (rescanRequired || geometryStatus.trim().lowercase() != "ready") return false
    if (source !in setOf(
            OneMapSource.CAMERA_CV_2D,
            OneMapSource.ROOMPLAN_LIDAR_3D,
            OneMapSource.ARKIT_VIDEO_3D
        )
    ) return false
    return hasRenderableGeometry()
}

internal fun OneRoomMap.hasRenderableGeometry(): Boolean =
    polygons.any { it.points.size >= 3 } ||
        walls.any { it.start != it.end } ||
        furniture.any { it.size.x > 0f && it.size.y > 0f } ||
        openings.any { it.start != it.end }
