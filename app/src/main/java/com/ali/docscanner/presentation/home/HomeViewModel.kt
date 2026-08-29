package com.ali.docscanner.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ali.docscanner.data.repository.DocumentRepository
import com.ali.docscanner.domain.model.Document
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val RECENT_DOCUMENTS_LIMIT = 5

@HiltViewModel
class HomeViewModel @Inject constructor(
    documentRepository: DocumentRepository
) : ViewModel() {

    val recentDocuments: StateFlow<List<Document>> = documentRepository.observeDocuments()
        .map { it.take(RECENT_DOCUMENTS_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
