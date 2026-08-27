package com.ali.docscanner.presentation.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.util.ImageFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface ConfirmUiState {
    data object Reviewing : ConfirmUiState
    data object Saving : ConfirmUiState
    data class Saved(val filePath: String) : ConfirmUiState
    data class Error(val message: String) : ConfirmUiState
}

@HiltViewModel
class ConfirmViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val imagePath: String = Uri.decode(
        checkNotNull(savedStateHandle.get<String>("imagePath")) {
            "imagePath argument is required for the Confirm screen"
        }
    )

    private val _uiState = MutableStateFlow<ConfirmUiState>(ConfirmUiState.Reviewing)
    val uiState: StateFlow<ConfirmUiState> = _uiState.asStateFlow()

    fun confirmSave() {
        _uiState.value = ConfirmUiState.Saving
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    ImageFileManager.moveToPermanentStorage(appContext, File(imagePath))
                }
                _uiState.value = ConfirmUiState.Saved(saved.absolutePath)
            } catch (e: Exception) {
                _uiState.value = ConfirmUiState.Error(e.message ?: "Failed to save image")
            }
        }
    }

    /** User retakes or backs out — the unconfirmed temp file must not linger. */
    fun discard() {
        ImageFileManager.deleteFile(File(imagePath))
    }
}
