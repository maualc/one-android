package com.one.cognitivecompanion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.one.cognitivecompanion.ui.theme.OneBlue
import com.one.cognitivecompanion.ui.theme.OneMint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

private data class OneFaceCaptureStage(val title: String, val instruction: String)

private val oneFaceCaptureStages = listOf(
    OneFaceCaptureStage("Front view", "Look straight at the camera and keep your face inside the guide."),
    OneFaceCaptureStage("Right view", "Turn your head gently to one side while keeping both eyes visible."),
    OneFaceCaptureStage("Left view", "Turn your head to the other side and hold still.")
)

private enum class OneFaceEnrollmentCameraPosition(
    val label: String,
    val apiValue: String,
    val selector: CameraSelector
) {
    FRONT("Front camera", "front", CameraSelector.DEFAULT_FRONT_CAMERA),
    BACK("Back camera", "back", CameraSelector.DEFAULT_BACK_CAMERA)
}

private data class OneValidatedFaceFrame(val frame: OneFaceEnrollmentFrame, val yaw: Float)

@Composable
fun OneFaceEnrollmentDialog(
    recipientName: String,
    onDismiss: () -> Unit,
    onComplete: (List<OneFaceEnrollmentFrame>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(88)
            .build()
    }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var capturedFrames by remember { mutableStateOf<List<OneValidatedFaceFrame>>(emptyList()) }
    var capturing by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraPosition by remember { mutableStateOf(OneFaceEnrollmentCameraPosition.FRONT) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (!granted) error = "Camera permission is required to capture the three face views."
    }

    DisposableEffect(permissionGranted, lifecycleOwner, previewView, imageCapture, cameraPosition) {
        cameraReady = false
        if (!permissionGranted) return@DisposableEffect onDispose { }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener({
            runCatching {
                val cameraProvider = providerFuture.get()
                if (disposed) {
                    cameraProvider.unbindAll()
                    return@runCatching
                }
                provider = cameraProvider
                cameraProvider.unbindAll()
                if (!cameraProvider.hasCamera(cameraPosition.selector)) {
                    error = "${cameraPosition.label} is not available on this device."
                    return@runCatching
                }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraPosition.selector, preview, imageCapture)
                cameraReady = true
            }.onFailure {
                cameraReady = false
                error = "${cameraPosition.label} could not be started."
            }
        }, executor)
        onDispose { disposed = true; provider?.unbindAll() }
    }

    Dialog(
        onDismissRequest = { if (!capturing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (permissionGranted) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Camera access", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("ONE validates each view on this phone before it is submitted.", color = Color.White.copy(alpha = 0.75f))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !capturing,
                    modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) { Icon(Icons.Default.Close, contentDescription = "Close face setup", tint = Color.White) }

                if (permissionGranted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 245.dp, height = 320.dp)
                            .clip(RoundedCornerShape(120.dp))
                            .border(3.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(120.dp))
                            .background(Color.Transparent)
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val stage = oneFaceCaptureStages[capturedFrames.size.coerceAtMost(oneFaceCaptureStages.lastIndex)]
                            Text("FACE SETUP · ${capturedFrames.size + 1} OF 3", color = OneBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(stage.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("Setting up recognition for $recipientName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OneFaceEnrollmentCameraPosition.values().forEach { position ->
                                    val selectCamera = {
                                        if (cameraPosition != position) {
                                            cameraPosition = position
                                            capturedFrames = emptyList()
                                            error = null
                                        }
                                    }
                                    if (cameraPosition == position) {
                                        Button(
                                            onClick = selectCamera,
                                            enabled = !capturing,
                                            modifier = Modifier.weight(1f)
                                        ) { Text(position.label) }
                                    } else {
                                        OutlinedButton(
                                            onClick = selectCamera,
                                            enabled = !capturing,
                                            modifier = Modifier.weight(1f)
                                        ) { Text(position.label) }
                                    }
                                }
                            }
                            Text(stage.instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(3) { index ->
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (index < capturedFrames.size) OneMint else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (capturedFrames.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { capturedFrames = capturedFrames.dropLast(1); error = null },
                                        enabled = !capturing,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Retake") }
                                }
                                Button(
                                    onClick = {
                                        capturing = true
                                        error = null
                                        scope.launch {
                                            runCatching {
                                                captureAndValidateFace(context, imageCapture, capturedFrames, cameraPosition.apiValue)
                                            }.onSuccess { validated ->
                                                val updated = capturedFrames + validated
                                                capturedFrames = updated
                                                if (updated.size == 3) onComplete(updated.map { it.frame })
                                            }.onFailure { throwable ->
                                                error = throwable.message ?: "The face could not be validated. Try again in even light."
                                            }
                                            capturing = false
                                        }
                                    },
                                    enabled = !capturing && cameraReady,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (capturing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(Modifier.size(7.dp))
                                    Text(if (capturing) "Checking…" else "Capture")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun captureAndValidateFace(
    context: android.content.Context,
    imageCapture: ImageCapture,
    existingFrames: List<OneValidatedFaceFrame>,
    cameraPosition: String
): OneValidatedFaceFrame {
    val captured = suspendCancellableCoroutine<Pair<ByteArray, Int>> { continuation ->
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    try {
                        if (image.format != ImageFormat.JPEG) error("The camera returned an unsupported image format.")
                        val buffer = image.planes.first().buffer
                        val bytes = ByteArray(buffer.remaining()).also(buffer::get)
                        if (continuation.isActive) continuation.resume(bytes to image.imageInfo.rotationDegrees)
                    } catch (error: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
        )
    }
    return withContext(Dispatchers.Default) {
        val decoded = BitmapFactory.decodeByteArray(captured.first, 0, captured.first.size)
            ?: error("The camera image could not be decoded.")
        val rotated = if (captured.second == 0) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(captured.second.toFloat()) }, true
        )
        val maxSide = maxOf(rotated.width, rotated.height)
        val bitmap = if (maxSide > 1280) {
            val scale = 1280f / maxSide
            Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true)
        } else rotated
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
        val detector = FaceDetection.getClient(options)
        val faces = try { detector.process(InputImage.fromBitmap(bitmap, 0)).awaitResult() } finally { detector.close() }
        val face = faces.singleOrNull() ?: error(if (faces.isEmpty()) "No face was found. Move closer and try again." else "Only one person can appear in the frame.")
        validateFacePose(face, bitmap, existingFrames)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)
        val bytes = output.toByteArray()
        check(bytes.size <= 2_900_000) { "The captured image is too large. Try again." }
        OneValidatedFaceFrame(
            OneFaceEnrollmentFrame(Base64.encodeToString(bytes, Base64.NO_WRAP), bitmap.width, bitmap.height, cameraPosition),
            face.headEulerAngleY
        )
    }
}

private fun validateFacePose(face: Face, bitmap: Bitmap, existingFrames: List<OneValidatedFaceFrame>) {
    val coverage = face.boundingBox.width().toFloat() * face.boundingBox.height() / (bitmap.width.toFloat() * bitmap.height)
    check(coverage >= 0.06f) { "Move a little closer so your face fills the guide." }
    val yaw = face.headEulerAngleY
    when (existingFrames.size) {
        0 -> check(abs(yaw) <= 16f) { "Look straight at the camera for the first view." }
        1 -> check(abs(yaw) >= 10f) { "Turn your head gently for the second view." }
        2 -> {
            check(abs(yaw) >= 10f) { "Turn your head gently for the final view." }
            check(yaw * existingFrames[1].yaw < 0f) { "Turn to the opposite side for the final view." }
        }
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }
