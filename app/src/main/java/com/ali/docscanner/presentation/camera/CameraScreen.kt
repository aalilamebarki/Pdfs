package com.ali.docscanner.presentation.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.ali.docscanner.R
import com.ali.docscanner.util.PermissionStatus
import com.ali.docscanner.util.openAppSettings
import com.ali.docscanner.util.rememberCameraPermissionState
import java.io.FileOutputStream

private const val TAG = "CameraScreen"

@Composable
fun CameraScreen(
    onImageReady: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val permissionState = rememberCameraPermissionState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is CameraUiState.Captured) {
            onImageReady(state.filePath)
        }
    }

    when (permissionState.status) {
        PermissionStatus.GRANTED -> {
            CameraLiveContent(
                viewModel = viewModel,
                onClose = {
                    viewModel.cancelAndCleanUp()
                    onClose()
                }
            )
        }
        PermissionStatus.PERMANENTLY_DENIED -> {
            val context = LocalContext.current
            PermissionBlockedContent(
                onOpenSettings = { openAppSettings(context) },
                onClose = onClose
            )
        }
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> {
            PermissionRequestContent(
                onRequestPermission = permissionState.requestPermission,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(onRequestPermission: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.camera_permission_rationale),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.grant_permission))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        }
    }
}

@Composable
private fun PermissionBlockedContent(onOpenSettings: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.camera_permission_permanently_denied),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        }
    }
}

@Composable
private fun CameraLiveContent(
    viewModel: CameraViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(isFlashOn, isFrontCamera) {
        bindCameraUseCases(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            imageCapture = imageCapture,
            isFrontCamera = isFrontCamera,
            isFlashOn = isFlashOn
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        DocumentOverlay(modifier = Modifier.fillMaxSize())

        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Text(text = "\u2715", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.toggleFlash() }) {
                    Text(
                        text = if (isFlashOn) "\u26A1 ON" else "\u26A1 OFF",
                        color = Color.White
                    )
                }

                CaptureButton(
                    enabled = uiState !is CameraUiState.Error,
                    onClick = {
                        captureImage(
                            context = context,
                            imageCapture = imageCapture,
                            viewModel = viewModel
                        )
                    }
                )

                TextButton(onClick = { viewModel.toggleLensFacing() }) {
                    Text(text = "\u21BB", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GalleryImportButton(
                onImagePicked = { uri ->
                    importFromGallery(context = context, uri = uri, viewModel = viewModel)
                }
            )
        }

        val errorState = uiState
        if (errorState is CameraUiState.Error) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Text(
                    text = errorState.message,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = if (enabled) Color.White else Color.Gray,
        border = BorderStroke(4.dp, Color.LightGray),
        onClick = onClick,
        enabled = enabled
    ) {}
}

@Composable
private fun GalleryImportButton(onImagePicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onImagePicked)
    }

    TextButton(onClick = { launcher.launch("image/*") }) {
        Text(text = stringResource(R.string.import_from_gallery), color = Color.White)
    }
}

/**
 * Static visual guide only (not derived from live frame analysis). The actual
 * best-effort edge heuristic runs once on the captured still image — see
 * [com.ali.docscanner.util.ImageProcessor.detectDocumentBounds], shown on the Confirm
 * screen. Real-time frame analysis is heavier and out of scope for Phase 2.
 */
@Composable
private fun DocumentOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val marginX = size.width * 0.08f
        val marginTop = size.height * 0.15f
        val marginBottom = size.height * 0.30f
        drawRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(marginX, marginTop),
            size = Size(
                size.width - marginX * 2,
                size.height - marginTop - marginBottom
            ),
            style = Stroke(width = 4f)
        )
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    viewModel: CameraViewModel
) {
    val outputFile = viewModel.createTempCaptureFile()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                viewModel.onCaptureSuccess(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Capture failed", exception)
                viewModel.onCaptureError(exception.message ?: "Capture failed")
            }
        }
    )
}

private fun importFromGallery(
    context: Context,
    uri: Uri,
    viewModel: CameraViewModel
) {
    val tempFile = viewModel.createTempCaptureFile()
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to open selected image")
        viewModel.onGalleryImageImported(tempFile)
    } catch (e: Exception) {
        Log.e(TAG, "Gallery import failed", e)
        viewModel.onCaptureError(e.message ?: "Failed to import image")
    }
}

private fun bindCameraUseCases(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    isFrontCamera: Boolean,
    isFlashOn: Boolean
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture.flashMode = if (isFlashOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }

        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}
