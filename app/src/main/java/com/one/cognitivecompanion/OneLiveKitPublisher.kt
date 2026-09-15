package com.one.cognitivecompanion

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalAudioTrack
import io.livekit.android.room.track.LocalVideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.time.Instant

/** Publishes the Android device camera/microphone to a paired ONE LiveKit room. */
class OneLiveKitPublisherService : LifecycleService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var room: Room? = null
    private var videoTrack: LocalVideoTrack? = null
    private var audioTrack: LocalAudioTrack? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopPublishing()
            return START_NOT_STICKY
        }
        if (!hasRequiredPermissions()) {
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Connecting to secure live room"))
        serviceScope.launch { connectAndPublish() }
        return START_NOT_STICKY
    }

    private suspend fun connectAndPublish() {
        val secureStore = OneSecureStore(applicationContext)
        val reconnectable = secureStore.restorePublisherForReconnect()
        val session = when {
            reconnectable == null -> secureStore.restore()?.session
            reconnectable.session.expiresAt?.isBefore(Instant.now()) == true -> {
                runCatching {
                    OneHttpApiClient().reconnectCamera(
                        reconnectable.session.userId,
                        reconnectable.session.reconnectToken.orEmpty()
                    ).copy(reconnectToken = reconnectable.session.reconnectToken)
                }.onSuccess { refreshed ->
                    secureStore.saveSession(refreshed, onboardingComplete = true)
                }.getOrNull()
            }
            else -> reconnectable.session
        }
        if (session == null) {
            updateNotification("Sign in to publish this device")
            stopPublishing()
            return
        }
        if (session.role != OneRole.PUBLISHER && !session.backendRole.equals("publisher", ignoreCase = true)) {
            updateNotification("Publisher pairing is required for this device")
            stopPublishing()
            return
        }
        val publisherSession = if (session.reconnectToken.isNullOrBlank()) {
            runCatching {
                val link = OneHttpApiClient().createCameraReconnectLink(session)
                session.copy(reconnectToken = link.reconnectToken).also { refreshed ->
                    secureStore.saveSession(refreshed, onboardingComplete = true)
                }
            }.getOrDefault(session)
        } else {
            session
        }
        runCatching {
            val token = OneHttpApiClient().liveKitToken(publisherSession, mode = "publish")
            val liveRoom = LiveKit.create(applicationContext)
            liveRoom.connect(token.serverUrl, token.participantToken)
            val localVideo = liveRoom.localParticipant.createVideoTrack(name = "one-camera")
            val localAudio = liveRoom.localParticipant.createAudioTrack(name = "one-microphone")
            localVideo.start()
            localVideo.startCapture()
            localAudio.start()
            check(liveRoom.localParticipant.publishVideoTrack(localVideo)) { "LiveKit rejected the video track." }
            check(liveRoom.localParticipant.publishAudioTrack(localAudio)) { "LiveKit rejected the audio track." }
            room = liveRoom
            videoTrack = localVideo
            audioTrack = localAudio
        }.onSuccess {
            updateNotification("Live camera and microphone are active")
        }.onFailure {
            updateNotification(it.message ?: "LiveKit publishing failed")
            stopPublishing()
        }
    }

    private fun stopPublishing() {
        videoTrack?.stopCapture()
        videoTrack?.stop()
        videoTrack?.dispose()
        audioTrack?.stop()
        audioTrack?.dispose()
        room?.disconnect()
        room?.release()
        videoTrack = null
        audioTrack = null
        room = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopPublishing()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun hasRequiredPermissions(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.one_livekit_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows when this paired device publishes a LiveKit camera feed."
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("ONE")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        ContextCompat.getSystemService(this, NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START = "com.one.cognitivecompanion.action.START_LIVEKIT_PUBLISH"
        const val ACTION_STOP = "com.one.cognitivecompanion.action.STOP_LIVEKIT_PUBLISH"
        const val CHANNEL_ID = "one_livekit_publishing"
        const val NOTIFICATION_ID = 1_102

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, OneLiveKitPublisherService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OneLiveKitPublisherService::class.java))
        }
    }
}
