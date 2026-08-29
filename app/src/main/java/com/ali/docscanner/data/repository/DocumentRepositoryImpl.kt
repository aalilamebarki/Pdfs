package com.ali.docscanner.data.repository

import android.content.Context
import com.ali.docscanner.data.local.dao.DocumentDao
import com.ali.docscanner.data.local.dao.PageDao
import com.ali.docscanner.data.local.entity.DocumentEntity
import com.ali.docscanner.domain.model.Document
import com.ali.docscanner.util.ImageFileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    @ApplicationContext private val appContext: Context
) : DocumentRepository {

    override fun observeDocuments(): Flow<List<Document>> {
        return documentDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeDocumentCount(): Flow<Int> = documentDao.observeCount()

    override suspend fun insertDocument(name: String): Long {
        val now = System.currentTimeMillis()
        return documentDao.insert(
            DocumentEntity(
                name = name,
                createdAt = now,
                updatedAt = now,
                thumbnailPath = null,
                pageCount = 0
            )
        )
    }

    override suspend fun getDocumentById(id: Long): Document? {
        return documentDao.getById(id)?.toDomain()
    }

    override suspend fun renameDocument(id: Long, newName: String) {
        val existing = documentDao.getById(id) ?: return
        documentDao.update(existing.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun touchDocumentMeta(id: Long, pageCount: Int, thumbnailPath: String?) {
        val existing = documentDao.getById(id) ?: return
        documentDao.update(
            existing.copy(
                pageCount = pageCount,
                thumbnailPath = thumbnailPath ?: existing.thumbnailPath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteDocumentWithFiles(id: Long) {
        val document = documentDao.getById(id) ?: return
        val pages = pageDao.getPagesForDocument(id)

        withContext(Dispatchers.IO) {
            pages.forEach { page ->
                ImageFileManager.deleteFileAtPath(page.imagePath)
                page.thumbnailPath?.let { ImageFileManager.deleteFileAtPath(it) }
            }
            ImageFileManager.deleteExportedPdfFor(appContext, id)
        }

        documentDao.delete(document) // Room FK CASCADE removes the Page rows
    }

    private fun DocumentEntity.toDomain() = Document(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        pageCount = pageCount
    )
}

