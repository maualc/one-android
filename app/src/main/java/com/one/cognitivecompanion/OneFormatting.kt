package com.one.cognitivecompanion

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Render an ISO instant in the device's local time zone for human review. */
fun Instant.toHumanDateTime(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
        .withZone(zone)
        .format(this)

/** Keep wire identifiers such as `manual-zones` readable in the UI. */
fun String.toHumanCoordinateFrame(): String = when (lowercase(Locale.ROOT)) {
    "manual-zones" -> "Manual zones"
    "arcore-local" -> "ARCore local"
    "camera-cv-2d" -> "Camera-derived 2D"
    "roomplan" -> "RoomPlan"
    "android-import" -> "Android import"
    else -> replace('_', ' ').replace('-', ' ').replaceFirstChar { it.uppercase() }
}

fun countLabel(count: Int, singular: String): String =
    "$count ${if (count == 1) singular else "${singular}s"}"
