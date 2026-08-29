package com.ali.docscanner.data.repository

import com.ali.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow

interface PageRepository {
    fun observePagesForDocument(documentId: Long): Flow<List<Page>>
    suspend fun getPagesForDocument(documentId: Long): List<Page>
    suspend fun insertPage(documentId: Long, imagePath: String, thumbnailPath: String?, order: Int): Long
    suspend fun deletePage(page: Page)
    suspend fun reorderPages(pages: List<Page>)
}
