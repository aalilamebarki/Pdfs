package com.ali.docscanner.presentation.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ali.docscanner.util.ImageFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

sealed interface CameraUiState {
    data object Preview : CameraUiState
    data class Captured(val filePath: String) : CameraUiState
    data class Error(val message: String) : CameraUiState
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Preview)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    /** Tracks the current unconfirmed temp file so it can be cleaned up if abandoned. */
    private var pendingTempFile: File? = null

    fun createTempCaptureFile(): File {
        val file = ImageFileManager.createTempCaptureFile(appContext)
        pendingTempFile = file
        return file
    }

    fun onCaptureSuccess(file: File) {
        pendingTempFile = file
        _uiState.value = CameraUiState.Captured(file.absolutePath)
    }

    fun onCaptureError(message: String) {
        _uiState.value = CameraUiState.Error(message)
    }

    fun onGalleryImageImported(file: File) {
        pendingTempFile = file
        _uiState.value = CameraUiState.Captured(file.absolutePath)
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun toggleLensFacing() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    /** User cancels out of the camera screen entirely (before confirming a page). */
    fun cancelAndCleanUp() {
        pendingTempFile?.let { ImageFileManager.deleteFile(it) }
        pendingTempFile = null
        ImageFileManager.clearTempCache(appContext)
        _uiState.value = CameraUiState.Preview
    }

    override fun onCleared() {
        super.onCleared()
        // Safety net for unexpected destruction while a capture is pending confirmation:
        // never leave orphaned temp files behind. Note: viewModelScope is already
        // cancelled by the time onCleared() runs, so this deletes synchronously — it's a
        // single small temp JPEG, negligible cost on the calling thread.
        pendingTempFile?.let { ImageFileManager.deleteFile(it) }
    }
}
