package com.ali.docscanner.presentation.filter

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ali.docscanner.R
import com.ali.docscanner.util.DocumentFilter
import com.ali.docscanner.util.FilterProcessor
import com.ali.docscanner.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PERMANENT_SAVE_QUALITY = 92

@Composable
fun FilterScreen(
    onSaved: (Long) -> Unit,
    onClose: () -> Unit,
    viewModel: FilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is FilterUiState.Saved) {
            onSaved(state.documentId)
        }
    }

    // FIX (audit): same system-back cleanup gap as Camera/Crop screens.
    BackHandler {
        viewModel.discardSource()
        onClose()
    }

    // FIX (audit): was decoding synchronously inside remember{} on the main thread.
    // Now decoded off-thread via produceState + Dispatchers.IO, with a loading state.
    val sourceBitmapState = produceState<Bitmap?>(initialValue = null, viewModel.imagePath) {
        value = withContext(Dispatchers.IO) {
            ImageProcessor.decodeSampledBitmap(File(viewModel.imagePath))
        }
    }
    val sourceBitmap = sourceBitmapState.value

    if (sourceBitmap == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedFilter by remember { mutableStateOf(DocumentFilter.ORIGINAL) }
    // NOTE (audit): filter application (ColorMatrix / per-pixel threshold) still runs
    // synchronously on the main thread on each selection tap. For the current MVP image
    // sizes (downsampled to <=1600px) this is usually sub-frame-budget but is a real,
    // documented jank risk on slower devices with large images — not fixed in this pass
    // to avoid introducing debounce/cancellation complexity without the ability to test
    // it on a real device here.
    val previewBitmap = remember(sourceBitmap, selectedFilter) {
        FilterProcessor.apply(sourceBitmap, selectedFilter)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.captured_page),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (uiState is FilterUiState.Saving) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val errorState = uiState
            if (errorState is FilterUiState.Error) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = Color.Black.copy(alpha = 0.85f)
                ) {
                    Text(text = errorState.message, color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }
        }

        FilterThumbnailRow(
            sourceBitmap = sourceBitmap,
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    viewModel.discardSource()
                    onClose()
                },
                enabled = uiState !is FilterUiState.Saving
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = {
                    val finalBitmap = FilterProcessor.apply(sourceBitmap, selectedFilter)
                    viewModel.confirmPage(finalBitmap, PERMANENT_SAVE_QUALITY)
                },
                enabled = uiState !is FilterUiState.Saving
            ) {
                Text(stringResource(R.string.save_page))
            }
        }
    }
}

@Composable
private fun FilterThumbnailRow(
    sourceBitmap: Bitmap,
    selectedFilter: DocumentFilter,
    onFilterSelected: (DocumentFilter) -> Unit
) {
    val thumbnailSource = remember(sourceBitmap) {
        val maxDim = 200
        val scale = maxDim.toFloat() / maxOf(sourceBitmap.width, sourceBitmap.height)
        if (scale >= 1f) sourceBitmap else Bitmap.createScaledBitmap(
            sourceBitmap,
            (sourceBitmap.width * scale).toInt().coerceAtLeast(1),
            (sourceBitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(DocumentFilter.values().toList()) { filter ->
            val thumb = remember(thumbnailSource, filter) { FilterProcessor.apply(thumbnailSource, filter) }
            val isSelected = filter == selectedFilter

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFilterSelected(filter) }
            ) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = filterLabel(filter),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Text(
                    text = filterLabel(filter),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun filterLabel(filter: DocumentFilter): String = when (filter) {
    DocumentFilter.ORIGINAL -> stringResource(R.string.filter_original)
    DocumentFilter.GRAYSCALE -> stringResource(R.string.filter_grayscale)
    DocumentFilter.BLACK_AND_WHITE -> stringResource(R.string.filter_bw)
    DocumentFilter.ENHANCED -> stringResource(R.string.filter_enhanced)
}
