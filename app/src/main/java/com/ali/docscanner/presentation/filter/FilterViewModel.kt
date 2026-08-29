package com.ali.docscanner.presentation.filter

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.R
import com.ali.docscanner.data.repository.DocumentRepository
import com.ali.docscanner.data.repository.PageRepository
import com.ali.docscanner.util.ImageFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface FilterUiState {
    data object Editing : FilterUiState
    data object Saving : FilterUiState
    data class Saved(val documentId: Long) : FilterUiState
    data class Error(val message: String) : FilterUiState
}

@HiltViewModel
class FilterViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val documentRepository: DocumentRepository,
    private val pageRepository: PageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val imagePath: String = Uri.decode(
        checkNotNull(savedStateHandle.get<String>("imagePath")) {
            "imagePath argument is required for the Filter screen"
        }
    )

    /** 0L is the sentinel meaning "no document created yet — this is the first page". */
    val incomingDocumentId: Long = checkNotNull(savedStateHandle.get<String>("documentId")) {
        "documentId argument is required for the Filter screen"
    }.toLong()

    private val _uiState = MutableStateFlow<FilterUiState>(FilterUiState.Editing)
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    /**
     * Saves the already-filtered [finalBitmap] (composed by the screen from the source +
     * selected [com.ali.docscanner.util.DocumentFilter]) as a confirmed page.
     */
    fun confirmPage(finalBitmap: android.graphics.Bitmap, quality: Int) {
        _uiState.value = FilterUiState.Saving
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    ImageFileManager.savePermanentBitmap(appContext, finalBitmap, quality)
                }

                val documentId = if (incomingDocumentId == 0L) {
                    val name = "Scan ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}"
                    documentRepository.insertDocument(name)
                } else {
                    incomingDocumentId
                }

                val existingPages = pageRepository.getPagesForDocument(documentId)
                pageRepository.insertPage(
                    documentId = documentId,
                    imagePath = saved.imagePath,
                    thumbnailPath = saved.thumbnailPath,
                    order = existingPages.size
                )

                val newPageCount = existingPages.size + 1
                val thumbnailForDocument = if (existingPages.isEmpty()) saved.thumbnailPath else null
                documentRepository.touchDocumentMeta(documentId, newPageCount, thumbnailForDocument)

                withContext(Dispatchers.IO) { ImageFileManager.deleteFileAtPath(imagePath) }

                _uiState.value = FilterUiState.Saved(documentId)
            } catch (e: Exception) {
                _uiState.value = FilterUiState.Error(e.message ?: appContext.getString(R.string.error_saving_page))
            }
        }
    }

    fun discardSource() {
        viewModelScope.launch(Dispatchers.IO) {
            ImageFileManager.deleteFileAtPath(imagePath)
        }
    }
}
