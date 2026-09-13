package com.one.cognitivecompanion

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX capture pipeline. Frames are sampled (rather than continuously
 * uploaded), compressed in memory and sent to the bounded vision endpoint. No
 * image is written to disk by this service.
 */
class OneCaptureService : LifecycleService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var uploadJob: Job? = null
    private var cameraId: UUID? = null
    private var candidateLabels: List<String> = DEFAULT_LABELS
    private var lastUploadAt = 0L
    private var sentFrames = 0
    private val knownObjectIds = mutableMapOf<String, UUID>()
    private var objectsLoaded = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            return START_NOT_STICKY
        }
        val requestedCamera = intent?.getStringExtra(EXTRA_CAMERA_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (requestedCamera != null) cameraId = requestedCamera
        intent?.getStringArrayListExtra(EXTRA_CANDIDATE_LABELS)?.let { values ->
            candidateLabels = values.map(String::trim).filter(String::isNotBlank).distinct().take(MAX_LABELS).ifEmpty { DEFAULT_LABELS }
        }
        if (cameraId == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!hasCameraPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting camera capture"))
        bindCamera()
        return START_NOT_STICKY
    }

    private fun bindCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                cameraProvider = provider
                provider.unbindAll()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor, ::analyze)
                val selector = when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> throw IllegalStateException("No camera is available")
                }
                provider.bindToLifecycle(this, selector, analysis)
                updateNotification("Camera capture is active")
            }.onFailure {
                updateNotification("Camera capture unavailable")
                stopCapture()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastUploadAt < FRAME_INTERVAL_MS) {
            image.close()
            return
        }
        lastUploadAt = now
        val bytes = image.toJpeg() ?: run {
            image.close()
            return
        }
        val width = image.width
        val height = image.height
        image.close()
        val selectedCamera = cameraId ?: return
        uploadJob?.cancel()
        uploadJob = serviceScope.launch {
            val session = OneSecureStore(applicationContext).restore()?.session ?: return@launch
            val result = runCatching {
                OneHttpApiClient().ingestVisionFrame(
                    session = session,
                    cameraId = selectedCamera,
                    frameBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    width = width,
                    height = height,
                    candidateLabels = candidateLabels,
                    capturedAt = Instant.now()
                )
            }
            result.onSuccess { frameResult ->
                sentFrames += 1
                serviceScope.launch {
                    persistDetections(session, selectedCamera, frameResult)
                }
                updateNotification("Camera active · $sentFrames frames · ${frameResult.detections.size} detections")
            }.onFailure {
                updateNotification("Camera active · waiting for network")
            }
        }
    }

    private suspend fun persistDetections(session: OneSession, selectedCamera: UUID, result: OneVisionFrameResult) {
        if (result.detections.isEmpty()) return
        if (!objectsLoaded) {
            runCatching { OneHttpApiClient().homeObjects(session) }
                .onSuccess { objects ->
                    objects.forEach { knownObjectIds[it.label.lowercase()] = it.id }
                    objectsLoaded = true
                }
        }
        val api = OneHttpApiClient()
        result.detections.forEach { detection ->
            val key = detection.label.lowercase()
            val objectId = knownObjectIds[key] ?: runCatching {
                api.createObject(session, OneObjectRequest(label = detection.label, displayName = detection.label))
            }.getOrNull()?.id?.also { knownObjectIds[key] = it } ?: return@forEach
            val world = detection.worldPoint
            runCatching {
                api.submitObservation(
                    session,
                    OneObservationRequest(
                        objectId = objectId,
                        cameraId = selectedCamera,
                        x = world?.getOrNull(0),
                        y = world?.getOrNull(1),
                        z = world?.getOrNull(2),
                        uncertaintyM = detection.uncertaintyM,
                        confidence = detection.confidence,
                        detectorVersion = result.detectorVersion
                    )
                )
            }
        }
    }

    private fun stopCapture() {
        cameraProvider?.unbindAll()
        uploadJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdownNow()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.one_camera_capture_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows when ONE is sampling the household camera."
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun ImageProxy.toJpeg(): ByteArray? {
        if (format != ImageFormat.YUV_420_888 || planes.size < 3) return null
        val nv21 = yuv420ToNv21(this) ?: return null
        val output = ByteArrayOutputStream()
        if (!YuvImage(nv21, ImageFormat.NV21, width, height, null).compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, output)) return null
        return output.toByteArray().takeIf { it.isNotEmpty() }
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray? {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val output = ByteArray(image.width * image.height * 3 / 2)
        var outputIndex = 0
        val yBuffer = yPlane.buffer
        val yRow = ByteArray(yPlane.rowStride)
        for (row in 0 until image.height) {
            val rowLength = minOf(image.width, yBuffer.remaining())
            yBuffer.get(yRow, 0, rowLength)
            yRow.copyInto(output, outputIndex, 0, rowLength)
            outputIndex += rowLength
            if (yPlane.rowStride > rowLength) yBuffer.position(minOf(yBuffer.position() + yPlane.rowStride - rowLength, yBuffer.limit()))
        }
        val chromaHeight = image.height / 2
        val chromaWidth = image.width / 2
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val uPixel = uPlane.pixelStride
        val vPixel = vPlane.pixelStride
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uIndex = row * uPlane.rowStride + col * uPixel
                val vIndex = row * vPlane.rowStride + col * vPixel
                if (uIndex >= uBuffer.limit() || vIndex >= vBuffer.limit() || outputIndex + 1 >= output.size) return null
                output[outputIndex++] = vBuffer.get(vIndex)
                output[outputIndex++] = uBuffer.get(uIndex)
            }
        }
        return output
    }

    companion object {
        const val ACTION_START = "com.one.cognitivecompanion.action.START_CAPTURE"
        const val ACTION_STOP = "com.one.cognitivecompanion.action.STOP_CAPTURE"
        const val EXTRA_CAMERA_ID = "camera_id"
        const val EXTRA_CANDIDATE_LABELS = "candidate_labels"
        const val CHANNEL_ID = "one_camera_capture"
        const val NOTIFICATION_ID = 1_101
        const val FRAME_INTERVAL_MS = 2_500L
        const val JPEG_QUALITY = 70
        const val MAX_LABELS = 20
        val DEFAULT_LABELS = listOf("keys", "glasses", "mug", "wallet", "phone")

        fun start(context: Context, cameraId: UUID, candidateLabels: List<String> = DEFAULT_LABELS) {
            val intent = Intent(context, OneCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CAMERA_ID, cameraId.toString())
                putStringArrayListExtra(EXTRA_CANDIDATE_LABELS, ArrayList(candidateLabels))
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OneCaptureService::class.java))
        }
    }
}
