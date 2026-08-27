package com.ali.docscanner.presentation.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ali.docscanner.R
import com.ali.docscanner.util.ImageProcessor
import java.io.File

@Composable
fun ConfirmScreen(
    onSaved: () -> Unit,
    onRetake: () -> Unit,
    viewModel: ConfirmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ConfirmUiState.Saved) {
            onSaved()
        }
    }

    val bitmapState = produceState<Bitmap?>(initialValue = null, viewModel.imagePath) {
        value = try {
            ImageProcessor.decodeSampledBitmap(File(viewModel.imagePath))
        } catch (e: Exception) {
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val bitmap = bitmapState.value
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.captured_page),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                val bounds = remember(bitmap) { ImageProcessor.detectDocumentBounds(bitmap) }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(
                            bounds.left * size.width,
                            bounds.top * size.height
                        ),
                        size = Size(
                            (bounds.right - bounds.left) * size.width,
                            (bounds.bottom - bounds.top) * size.height
                        ),
                        style = Stroke(width = 4f)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val errorState = uiState
            if (errorState is ConfirmUiState.Error) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.discard()
                    onRetake()
                },
                enabled = uiState !is ConfirmUiState.Saving
            ) {
                Text(stringResource(R.string.retake))
            }

            Button(
                onClick = { viewModel.confirmSave() },
                enabled = uiState !is ConfirmUiState.Saving
            ) {
                if (uiState is ConfirmUiState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
