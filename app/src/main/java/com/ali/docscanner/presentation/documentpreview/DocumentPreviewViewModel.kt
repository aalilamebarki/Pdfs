package com.ali.docscanner.presentation.documentpreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.data.repository.DocumentRepository
import com.ali.docscanner.data.repository.PageRepository
import com.ali.docscanner.domain.model.Page
import com.ali.docscanner.util.ImageFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface DocumentPreviewEvent {
    data object DocumentDeleted : DocumentPreviewEvent
}

@HiltViewModel
class DocumentPreviewViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val pageRepository: PageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val documentId: Long = checkNotNull(savedStateHandle.get<String>("documentId")) {
        "documentId argument is required for the Document Preview screen"
    }.toLong()

    val pages: StateFlow<List<Page>> = pageRepository.observePagesForDocument(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableStateFlow<DocumentPreviewEvent?>(null)
    val events: StateFlow<DocumentPreviewEvent?> = _events.asStateFlow()

    fun moveUp(page: Page) = reorder(page, -1)
    fun moveDown(page: Page) = reorder(page, 1)

    private fun reorder(page: Page, delta: Int) {
        viewModelScope.launch {
            val current = pages.value.sortedBy { it.pageOrder }.toMutableList()
            val fromIndex = current.indexOfFirst { it.id == page.id }
            val toIndex = fromIndex + delta
            if (fromIndex < 0 || toIndex < 0 || toIndex >= current.size) return@launch
            val moved = current.removeAt(fromIndex)
            current.add(toIndex, moved)
            pageRepository.reorderPages(current)
        }
    }

    fun deletePage(page: Page) {
        viewModelScope.launch {
            pageRepository.deletePage(page)
            withContext(Dispatchers.IO) {
                ImageFileManager.deleteFileAtPath(page.imagePath)
                page.thumbnailPath?.let { ImageFileManager.deleteFileAtPath(it) }
            }

            val remaining = pageRepository.getPagesForDocument(documentId)
            if (remaining.isEmpty()) {
                documentRepository.deleteDocumentWithFiles(documentId)
                _events.value = DocumentPreviewEvent.DocumentDeleted
            } else {
                documentRepository.touchDocumentMeta(
                    id = documentId,
                    pageCount = remaining.size,
                    thumbnailPath = remaining.minByOrNull { it.pageOrder }?.thumbnailPath
                )
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
