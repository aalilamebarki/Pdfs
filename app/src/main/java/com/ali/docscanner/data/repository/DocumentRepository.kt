package com.ali.docscanner.data.repository

import com.ali.docscanner.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeDocuments(): Flow<List<Document>>
    fun observeDocumentCount(): Flow<Int>
    suspend fun insertDocument(name: String): Long
}
