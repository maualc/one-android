package com.one.cognitivecompanion

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ar.core.ArCoreApk
import com.one.cognitivecompanion.ui.theme.OneAmber
import com.one.cognitivecompanion.ui.theme.OneBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal enum class OneArCoreStatus {
    CHECKING,
    AVAILABLE_UNCONFIRMED,
    UNAVAILABLE
}

internal data class OneArCoreCapability(
    val status: OneArCoreStatus,
    val detail: String
)

/**
 * ARCore is optional in ONE. This probe checks device availability only.
 * Depth must be checked inside an installed ARCore capture session before a
 * native 3D mode is offered; this release never claims a metric 3D map.
 */
internal fun inspectOneArCore(context: Context): OneArCoreCapability {
    val availability = runCatching {
        ArCoreApk.getInstance().checkAvailability(context)
    }.getOrNull() ?: return OneArCoreCapability(
        OneArCoreStatus.UNAVAILABLE,
        "ARCore could not be checked on this device."
    )
    if (availability.name.startsWith("UNKNOWN")) {
        return OneArCoreCapability(
            OneArCoreStatus.CHECKING,
            "ARCore support is still being checked. The 2D RGB scan is available now."
        )
    }
    if (!availability.isSupported) {
        return OneArCoreCapability(
            OneArCoreStatus.UNAVAILABLE,
            "ARCore is not available. The bounded 2D RGB scan remains the supported fallback."
        )
    }
    return OneArCoreCapability(
        OneArCoreStatus.AVAILABLE_UNCONFIRMED,
        "ARCore is supported. Depth and room geometry still require an Android 3D capture session; the current room walkthrough creates an approximate 2D map."
    )
}

@Composable
internal fun OneArCoreCapabilityCard() {
    val context = LocalContext.current.applicationContext
    var capability by remember(context) {
        mutableStateOf(OneArCoreCapability(OneArCoreStatus.CHECKING, "Checking this device…"))
    }
    LaunchedEffect(context) {
        repeat(5) {
            capability = withContext(Dispatchers.IO) { inspectOneArCore(context) }
            if (capability.status != OneArCoreStatus.CHECKING) return@LaunchedEffect
            delay(1_000)
        }
    }
    val tint = when (capability.status) {
        OneArCoreStatus.CHECKING -> OneBlue
        OneArCoreStatus.AVAILABLE_UNCONFIRMED -> OneAmber
        OneArCoreStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                when (capability.status) {
                    OneArCoreStatus.CHECKING -> "3D capability"
                    OneArCoreStatus.AVAILABLE_UNCONFIRMED -> "ARCore available"
                    OneArCoreStatus.UNAVAILABLE -> "2D fallback active"
                },
                style = MaterialTheme.typography.titleSmall,
                color = tint
            )
            if (capability.status == OneArCoreStatus.CHECKING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            Text(
                capability.detail,
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
