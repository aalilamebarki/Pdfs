package com.ali.docscanner.presentation.documentslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.data.repository.DocumentRepository
import com.ali.docscanner.domain.model.Document
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsListViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    val documents: StateFlow<List<Document>> = documentRepository.observeDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            documentRepository.deleteDocumentWithFiles(document.id)
        }
    }
}
