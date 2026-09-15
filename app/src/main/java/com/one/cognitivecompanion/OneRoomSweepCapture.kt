package com.one.cognitivecompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Base64
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Fixed, bounded dimensions shared by the client and the backend job contract. */
object OneRoomSweepCaptureConfig {
    const val TARGET_WIDTH = 640
    const val TARGET_HEIGHT = 480
    const val MAX_FRAME_COUNT = 20
    const val MIN_FRAME_COUNT = 3
    const val DURATION_MS = 14_000L
    const val FRAME_INTERVAL_MS = 700L
    const val JPEG_QUALITY = 58
    const val MAX_FRAME_BYTES = 3_000_000
    const val MAX_BATCH_BYTES = 18_000_000
}

/**
 * Small CameraX controller for a finite room walkthrough.
 *
 * The controller keeps only the bounded encoded frames needed by the map
 * endpoint. It does not persist camera images to disk and it never feeds this
 * walkthrough into the live object detector.
 */
internal class OneRoomSweepCameraController(context: Context) {
    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val lock = Any()
    private val frames = ArrayList<OneMapGenerationFrame>(OneRoomSweepCaptureConfig.MAX_FRAME_COUNT)
    private val previewView = PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var isBound = false
    private var closed = false
    private var capturing = false
    private var startedAtMs = 0L
    private var lastCapturedAtMs = 0L
    private var batchBytes = 0

    @Volatile
    var onStateChanged: (capturing: Boolean, frameCount: Int) -> Unit = { _, _ -> }

    @Volatile
    var onComplete: (frames: List<OneMapGenerationFrame>) -> Unit = {}

    @Volatile
    var onError: (message: String) -> Unit = {}

    val view: PreviewView
        get() = previewView

    fun bind(lifecycleOwner: LifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener({
            if (closed) return@addListener
            try {
                val provider = providerFuture.get()
                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(
                        Size(
                            OneRoomSweepCaptureConfig.TARGET_WIDTH,
                            OneRoomSweepCaptureConfig.TARGET_HEIGHT
                        )
                    )
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { image -> analyse(image) }
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                cameraProvider = provider
                isBound = true
            } catch (error: Exception) {
                notifyError("Could not start the room scan camera.")
            }
        }, mainExecutor)
    }

    fun start() {
        val canStart = synchronized(lock) {
            if (!isBound || closed || capturing) {
                false
            } else {
                frames.clear()
                batchBytes = 0
                startedAtMs = SystemClock.elapsedRealtime()
                lastCapturedAtMs = 0L
                capturing = true
                true
            }
        }
        if (!canStart) {
            if (!isBound) notifyError("The camera is still starting. Try again in a moment.")
            return
        }
        notifyState(true, 0)
    }

    fun stop() {
        synchronized(lock) {
            capturing = false
            frames.clear()
            batchBytes = 0
        }
        notifyState(false, 0)
    }

    fun close() {
        synchronized(lock) {
            closed = true
            capturing = false
            frames.clear()
            batchBytes = 0
        }
        mainExecutor.execute {
            cameraProvider?.unbindAll()
            cameraProvider = null
        }
        analysisExecutor.shutdownNow()
    }

    private fun analyse(image: ImageProxy) {
        var shouldEncode = false
        var completedFrames: List<OneMapGenerationFrame>? = null
        var immediateError: String? = null
        val now = SystemClock.elapsedRealtime()

        synchronized(lock) {
            if (capturing && now - startedAtMs >= OneRoomSweepCaptureConfig.DURATION_MS) {
                if (frames.size >= OneRoomSweepCaptureConfig.MIN_FRAME_COUNT) {
                    completedFrames = frames.toList()
                } else {
                    immediateError = "The room scan needs at least three usable frames. Move the phone slowly and try again."
                }
                capturing = false
            } else if (
                capturing &&
                now - lastCapturedAtMs >= OneRoomSweepCaptureConfig.FRAME_INTERVAL_MS &&
                frames.size < OneRoomSweepCaptureConfig.MAX_FRAME_COUNT
            ) {
                lastCapturedAtMs = now
                shouldEncode = true
            }
        }

        try {
            if (shouldEncode) {
                val jpeg = image.toRoomSweepJpeg()
                if (jpeg == null) {
                    // A single unusable camera frame is ignored. The next
                    // eligible frame can still complete the bounded sweep.
                } else {
                    synchronized(lock) {
                        if (capturing) {
                            when {
                                jpeg.size > OneRoomSweepCaptureConfig.MAX_FRAME_BYTES -> {
                                    immediateError = "A camera frame was too large. Lower the camera resolution and try again."
                                    capturing = false
                                }
                                batchBytes + jpeg.size > OneRoomSweepCaptureConfig.MAX_BATCH_BYTES -> {
                                    immediateError = "The room scan exceeded its in-memory size limit. Try again with a shorter sweep."
                                    capturing = false
                                }
                                else -> {
                                    frames += OneMapGenerationFrame(
                                        frameBase64 = Base64.encodeToString(jpeg, Base64.NO_WRAP),
                                        width = OneRoomSweepCaptureConfig.TARGET_WIDTH,
                                        height = OneRoomSweepCaptureConfig.TARGET_HEIGHT,
                                        capturedAt = Instant.now()
                                    )
                                    batchBytes += jpeg.size
                                    val frameCount = frames.size
                                    if (frameCount >= OneRoomSweepCaptureConfig.MAX_FRAME_COUNT) {
                                        completedFrames = frames.toList()
                                        capturing = false
                                    }
                                    notifyState(capturing, frameCount)
                                }
                            }
                        }
                    }
                }
            }
        } catch (error: Exception) {
            synchronized(lock) { capturing = false }
            immediateError = "The camera could not encode this room frame. Try the walkthrough again."
        } finally {
            image.close()
        }

        if (immediateError != null) {
            synchronized(lock) {
                frames.clear()
                batchBytes = 0
            }
            notifyState(false, 0)
            notifyError(immediateError.orEmpty())
        } else if (completedFrames != null) {
            val result = completedFrames.orEmpty()
            synchronized(lock) {
                frames.clear()
                batchBytes = 0
            }
            notifyState(false, result.size)
            mainExecutor.execute { onComplete(result) }
        }
    }

    private fun notifyState(isCapturing: Boolean, count: Int) {
        mainExecutor.execute { onStateChanged(isCapturing, count) }
    }

    private fun notifyError(message: String) {
        mainExecutor.execute { onError(message) }
    }
}

@Composable
internal fun RoomSweepCapturePanel(
    onFramesReady: (List<OneMapGenerationFrame>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFramesReady by rememberUpdatedState(onFramesReady)
    val currentOnCancel by rememberUpdatedState(onCancel)
    var frameCount by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val controller = remember(context) { OneRoomSweepCameraController(context) }

    DisposableEffect(controller, lifecycleOwner) {
        controller.onStateChanged = { active, count ->
            isCapturing = active
            frameCount = count
        }
        controller.onError = { message ->
            isCapturing = false
            cameraError = message
        }
        controller.onComplete = { frames ->
            isCapturing = false
            currentOnFramesReady(frames)
        }
        controller.bind(lifecycleOwner)
        onDispose { controller.close() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("Room walkthrough", style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep the phone in landscape and move slowly around the room. ONE captures up to 20 temporary RGB frames for an approximate 2D layout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AndroidView(
                factory = { controller.view },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            if (isCapturing) {
                LinearProgressIndicator(
                    progress = { (frameCount.toFloat() / OneRoomSweepCaptureConfig.MAX_FRAME_COUNT).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Capturing $frameCount/${OneRoomSweepCaptureConfig.MAX_FRAME_COUNT} frames…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "The scan takes about 14 seconds and stays in memory until it is submitted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        cameraError = null
                        controller.start()
                    },
                    enabled = !isCapturing,
                    modifier = Modifier.weight(1f)
                ) { Text("Start scan") }
                OutlinedButton(
                    onClick = {
                        controller.stop()
                        currentOnCancel()
                    },
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
            }
            cameraError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun ImageProxy.toRoomSweepJpeg(): ByteArray? {
    if (format != ImageFormat.YUV_420_888 || planes.size < 3) return null
    val nv21 = yuv420ToNv21() ?: return null
    val rawOutput = ByteArrayOutputStream()
    if (!YuvImage(nv21, ImageFormat.NV21, width, height, null).compressToJpeg(
            Rect(0, 0, width, height),
            OneRoomSweepCaptureConfig.JPEG_QUALITY,
            rawOutput
        )
    ) return null
    val decoded = BitmapFactory.decodeByteArray(rawOutput.toByteArray(), 0, rawOutput.size()) ?: return null
    val oriented = if (imageInfo.rotationDegrees % 360 == 0) {
        decoded
    } else {
        Bitmap.createBitmap(
            decoded,
            0,
            0,
            decoded.width,
            decoded.height,
            Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) },
            true
        )
    }
    val targetRatio = OneRoomSweepCaptureConfig.TARGET_WIDTH.toFloat() / OneRoomSweepCaptureConfig.TARGET_HEIGHT
    val sourceRatio = oriented.width.toFloat() / oriented.height.coerceAtLeast(1)
    val cropWidth: Int
    val cropHeight: Int
    if (sourceRatio > targetRatio) {
        cropHeight = oriented.height
        cropWidth = (cropHeight * targetRatio).toInt().coerceIn(1, oriented.width)
    } else {
        cropWidth = oriented.width
        cropHeight = (cropWidth / targetRatio).toInt().coerceIn(1, oriented.height)
    }
    val cropLeft = ((oriented.width - cropWidth) / 2).coerceAtLeast(0)
    val cropTop = ((oriented.height - cropHeight) / 2).coerceAtLeast(0)
    val cropped = Bitmap.createBitmap(oriented, cropLeft, cropTop, cropWidth, cropHeight)
    val scaled = Bitmap.createScaledBitmap(
        cropped,
        OneRoomSweepCaptureConfig.TARGET_WIDTH,
        OneRoomSweepCaptureConfig.TARGET_HEIGHT,
        true
    )
    val output = ByteArrayOutputStream()
    return try {
        if (scaled.compress(Bitmap.CompressFormat.JPEG, OneRoomSweepCaptureConfig.JPEG_QUALITY, output)) {
            output.toByteArray().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    } finally {
        if (scaled !== cropped) scaled.recycle()
        if (cropped !== oriented) cropped.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
    }
}

private fun ImageProxy.yuv420ToNv21(): ByteArray? {
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]
    val chromaWidth = (width + 1) / 2
    val chromaHeight = (height + 1) / 2
    val output = ByteArray(width * height + chromaWidth * chromaHeight * 2)
    var outputIndex = 0

    val yBuffer = yPlane.buffer.duplicate()
    val yBase = yBuffer.position()
    for (row in 0 until height) {
        val rowStart = yBase + row * yPlane.rowStride
        if (rowStart < 0 || rowStart + width > yBuffer.limit()) return null
        yBuffer.position(rowStart)
        yBuffer.get(output, outputIndex, width)
        outputIndex += width
    }

    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()
    val uBase = uBuffer.position()
    val vBase = vBuffer.position()
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val uIndex = uBase + row * uPlane.rowStride + col * uPlane.pixelStride
            val vIndex = vBase + row * vPlane.rowStride + col * vPlane.pixelStride
            if (uIndex < 0 || vIndex < 0 || uIndex >= uBuffer.limit() || vIndex >= vBuffer.limit()) return null
            if (outputIndex + 1 >= output.size) return null
            output[outputIndex++] = vBuffer.get(vIndex)
            output[outputIndex++] = uBuffer.get(uIndex)
        }
    }
    return output
}
