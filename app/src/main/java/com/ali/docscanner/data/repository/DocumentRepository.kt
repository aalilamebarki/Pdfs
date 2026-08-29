package com.ali.docscanner.data.repository

import com.ali.docscanner.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeDocuments(): Flow<List<Document>>
    fun observeDocumentCount(): Flow<Int>
    suspend fun insertDocument(name: String): Long
    suspend fun getDocumentById(id: Long): Document?
    suspend fun renameDocument(id: Long, newName: String)

    /** Updates page count / thumbnail / updatedAt after a page is added or removed. */
    suspend fun touchDocumentMeta(id: Long, pageCount: Int, thumbnailPath: String?)

    /** Deletes the Document row (cascades to Page rows) AND their files on disk. */
    suspend fun deleteDocumentWithFiles(id: Long)
}

