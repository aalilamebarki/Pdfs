package com.ali.docscanner.presentation.pdfexport

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.R
import com.ali.docscanner.data.repository.DocumentRepository
import com.ali.docscanner.data.repository.PageRepository
import com.ali.docscanner.domain.model.Document
import com.ali.docscanner.util.ImageFileManager
import com.ali.docscanner.util.PdfGenerator
import com.ali.docscanner.util.PdfQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface PdfExportUiState {
    data object Loading : PdfExportUiState
    data class Ready(val document: Document, val pageCount: Int) : PdfExportUiState
    data object Generating : PdfExportUiState
    data class Generated(val pdfUri: Uri, val pdfPath: String) : PdfExportUiState
    data class Error(val message: String) : PdfExportUiState
}

@HiltViewModel
class PdfExportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val documentRepository: DocumentRepository,
    private val pageRepository: PageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val documentId: Long = checkNotNull(savedStateHandle.get<String>("documentId")) {
        "documentId argument is required for the PDF Export screen"
    }.toLong()

    private val _uiState = MutableStateFlow<PdfExportUiState>(PdfExportUiState.Loading)
    val uiState: StateFlow<PdfExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val document = documentRepository.getDocumentById(documentId)
            val pageCount = pageRepository.getPagesForDocument(documentId).size
            _uiState.value = if (document != null) {
                PdfExportUiState.Ready(document, pageCount)
            } else {
                PdfExportUiState.Error(appContext.getString(R.string.error_document_not_found))
            }
        }
    }

    fun renameDocument(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            documentRepository.renameDocument(documentId, newName.trim())
            val updated = documentRepository.getDocumentById(documentId) ?: return@launch
            val pageCount = pageRepository.getPagesForDocument(documentId).size
            _uiState.value = PdfExportUiState.Ready(updated, pageCount)
        }
    }

    fun generateAndPreparePdf(quality: PdfQuality) {
        _uiState.value = PdfExportUiState.Generating
        viewModelScope.launch {
            try {
                val pages = pageRepository.getPagesForDocument(documentId).sortedBy { it.pageOrder }
                if (pages.isEmpty()) {
                    _uiState.value = PdfExportUiState.Error(appContext.getString(R.string.error_document_has_no_pages))
                    return@launch
                }

                val outputFile = withContext(Dispatchers.IO) {
                    PdfGenerator.generate(
                        imagePaths = pages.map { it.imagePath },
                        outputFile = ImageFileManager.exportedPdfFile(appContext, documentId),
                        quality = quality
                    )
                }

                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    outputFile
                )

                _uiState.value = PdfExportUiState.Generated(uri, outputFile.absolutePath)
            } catch (e: Exception) {
                _uiState.value = PdfExportUiState.Error(e.message ?: appContext.getString(R.string.error_generating_pdf))
            }
        }
    }
}
