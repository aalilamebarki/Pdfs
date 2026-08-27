package com.ali.docscanner.data.repository

import com.ali.docscanner.data.local.dao.DocumentDao
import com.ali.docscanner.data.local.entity.DocumentEntity
import com.ali.docscanner.domain.model.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao
) : DocumentRepository {

    override fun observeDocuments(): Flow<List<Document>> {
        return documentDao.observeAll().map { list ->
            list.map {
                Document(
                    id = it.id,
                    name = it.name,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    thumbnailPath = it.thumbnailPath,
                    pageCount = it.pageCount
                )
            }
        }
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
}
