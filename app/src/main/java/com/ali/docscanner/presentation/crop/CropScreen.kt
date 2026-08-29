package com.ali.docscanner.presentation.crop

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ali.docscanner.R
import com.ali.docscanner.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot

@Composable
fun CropScreen(
    onCropped: (String, Long) -> Unit,
    onClose: () -> Unit,
    viewModel: CropViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is CropUiState.Done) {
            onCropped(state.filePath, viewModel.documentId)
        }
    }

    // FIX (audit): same system-back cleanup gap as CameraScreen — without this, leaving
    // Crop via gesture/hardware back orphaned the temp source file.
    BackHandler {
        viewModel.discardSource()
        onClose()
    }

    // FIX (audit): decoding used to happen synchronously inside remember{}, which runs
    // on the main thread during composition — real disk I/O + bitmap decode blocking the
    // UI thread. Now decoded off-thread via produceState + Dispatchers.IO.
    val baseBitmapState = produceState<Bitmap?>(initialValue = null, viewModel.imagePath) {
        value = withContext(Dispatchers.IO) {
            ImageProcessor.decodeSampledBitmap(File(viewModel.imagePath))
        }
    }
    val baseBitmap = baseBitmapState.value

    if (baseBitmap == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var rotationDegrees by remember { mutableStateOf(0) }
    val displayBitmap = remember(baseBitmap, rotationDegrees) {
        rotateBitmap(baseBitmap, rotationDegrees)
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var corners by remember(displayBitmap) {
        mutableStateOf(defaultCorners(displayBitmap))
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .aspectRatio(displayBitmap.width.toFloat() / displayBitmap.height.toFloat())
                .onSizeChanged { containerSize = it }
                .pointerInput(displayBitmap, containerSize) {
                    var activeIndex = -1
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (containerSize.width == 0) return@detectDragGestures
                            val scale = containerSize.width / displayBitmap.width.toFloat()
                            val screenPoints = corners.map { Offset(it.x * scale, it.y * scale) }
                            val nearestIndex = screenPoints.indices.minByOrNull { i ->
                                distance(screenPoints[i], startOffset)
                            } ?: -1
                            activeIndex = if (nearestIndex >= 0 &&
                                distance(screenPoints[nearestIndex], startOffset) < 96f
                            ) nearestIndex else -1
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeIndex < 0) return@detectDragGestures
                            val scale = containerSize.width / displayBitmap.width.toFloat()
                            val updated = corners.toMutableList()
                            val current = updated[activeIndex]
                            updated[activeIndex] = Offset(
                                (current.x + dragAmount.x / scale).coerceIn(0f, displayBitmap.width.toFloat()),
                                (current.y + dragAmount.y / scale).coerceIn(0f, displayBitmap.height.toFloat())
                            )
                            corners = updated
                        },
                        onDragEnd = { activeIndex = -1 }
                    )
                }
        ) {
            androidx.compose.foundation.Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.captured_page),
                modifier = Modifier.fillMaxSize()
            )

            if (containerSize.width > 0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scale = size.width / displayBitmap.width.toFloat()
                    val points = corners.map { Offset(it.x * scale, it.y * scale) }

                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        lineTo(points[1].x, points[1].y)
                        lineTo(points[2].x, points[2].y)
                        lineTo(points[3].x, points[3].y)
                        close()
                    }
                    drawPath(path, color = Color.Yellow, style = Stroke(width = 4f))
                    points.forEach { p ->
                        drawCircle(color = Color.Yellow.copy(alpha = 0.4f), radius = 28f, center = p)
                        drawCircle(color = Color.White, radius = 10f, center = p)
                    }
                }
            }

            if (uiState is CropUiState.Processing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val errorState = uiState
            if (errorState is CropUiState.Error) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = Color.Black.copy(alpha = 0.85f)
                ) {
                    Text(text = errorState.message, color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    viewModel.discardSource()
                    onClose()
                },
                enabled = uiState !is CropUiState.Processing
            ) {
                Text(stringResource(R.string.cancel))
            }

            TextButton(
                onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                enabled = uiState !is CropUiState.Processing
            ) {
                Text(stringResource(R.string.rotate))
            }

            Button(
                onClick = {
                    val flatCorners = FloatArray(8)
                    corners.forEachIndexed { i, offset ->
                        flatCorners[i * 2] = offset.x
                        flatCorners[i * 2 + 1] = offset.y
                    }
                    viewModel.applyPerspective(displayBitmap, flatCorners)
                },
                enabled = uiState !is CropUiState.Processing
            ) {
                Text(stringResource(R.string.apply_crop))
            }
        }
    }
}

private fun defaultCorners(bitmap: Bitmap): List<Offset> {
    val bounds = ImageProcessor.detectDocumentBounds(bitmap)
    return listOf(
        Offset(bounds.left * bitmap.width, bounds.top * bitmap.height),
        Offset(bounds.right * bitmap.width, bounds.top * bitmap.height),
        Offset(bounds.right * bitmap.width, bounds.bottom * bitmap.height),
        Offset(bounds.left * bitmap.width, bounds.bottom * bitmap.height)
    )
}

private fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return source
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun distance(a: Offset, b: Offset): Float =
    hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
