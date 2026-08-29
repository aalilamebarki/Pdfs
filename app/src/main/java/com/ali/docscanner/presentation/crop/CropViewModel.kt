package com.ali.docscanner.presentation.crop

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.R
import com.ali.docscanner.util.ImageFileManager
import com.ali.docscanner.util.PerspectiveTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject

sealed interface CropUiState {
    data object Editing : CropUiState
    data object Processing : CropUiState
    data class Done(val filePath: String) : CropUiState
    data class Error(val message: String) : CropUiState
}

@HiltViewModel
class CropViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val imagePath: String = Uri.decode(
        checkNotNull(savedStateHandle.get<String>("imagePath")) {
            "imagePath argument is required for the Crop screen"
        }
    )

    val documentId: Long = checkNotNull(savedStateHandle.get<String>("documentId")) {
        "documentId argument is required for the Crop screen"
    }.toLong()

    private val _uiState = MutableStateFlow<CropUiState>(CropUiState.Editing)
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()

    fun applyPerspective(sourceBitmap: Bitmap, corners: FloatArray) {
        _uiState.value = CropUiState.Processing
        viewModelScope.launch {
            try {
                val outputPath = withContext(Dispatchers.Default) {
                    val warped = PerspectiveTransformer.warp(sourceBitmap, corners)
                    val tempFile = ImageFileManager.createTempFile(appContext, "cropped")
                    FileOutputStream(tempFile).use {
                        warped.compress(Bitmap.CompressFormat.JPEG, 92, it)
                    }
                    if (warped !== sourceBitmap) warped.recycle()
                    tempFile.absolutePath
                }
                // FIX (audit): the original raw capture was never cleaned up on the
                // success path — only on cancel. Every completed scan was leaking one
                // full-resolution temp photo into cacheDir permanently.
                withContext(Dispatchers.IO) {
                    ImageFileManager.deleteFileAtPath(imagePath)
                }
                _uiState.value = CropUiState.Done(outputPath)
            } catch (e: Exception) {
                _uiState.value = CropUiState.Error(e.message ?: appContext.getString(R.string.error_processing_image))
            }
        }
    }

    /** Called when the user cancels out of Crop entirely (not just retaking a photo). */
    fun discardSource() {
        viewModelScope.launch(Dispatchers.IO) {
            ImageFileManager.deleteFileAtPath(imagePath)
        }
    }
}
