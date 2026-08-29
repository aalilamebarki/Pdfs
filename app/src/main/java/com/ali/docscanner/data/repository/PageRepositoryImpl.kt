package com.ali.docscanner.data.repository

import com.ali.docscanner.data.local.dao.PageDao
import com.ali.docscanner.data.local.entity.PageEntity
import com.ali.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PageRepositoryImpl @Inject constructor(
    private val pageDao: PageDao
) : PageRepository {

    override fun observePagesForDocument(documentId: Long): Flow<List<Page>> {
        return pageDao.observePagesForDocument(documentId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPagesForDocument(documentId: Long): List<Page> {
        return pageDao.getPagesForDocument(documentId).map { it.toDomain() }
    }

    override suspend fun insertPage(
        documentId: Long,
        imagePath: String,
        thumbnailPath: String?,
        order: Int
    ): Long {
        return pageDao.insert(
            PageEntity(
                documentId = documentId,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                pageOrder = order,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePage(page: Page) {
        pageDao.delete(page.toEntity())
    }

    override suspend fun reorderPages(pages: List<Page>) {
        pages.forEachIndexed { index, page ->
            pageDao.update(page.toEntity().copy(pageOrder = index))
        }
    }

    private fun PageEntity.toDomain() = Page(
        id = id,
        documentId = documentId,
        imagePath = imagePath,
        thumbnailPath = thumbnailPath,
        pageOrder = pageOrder,
        createdAt = createdAt
    )

    private fun Page.toEntity() = PageEntity(
        id = id,
        documentId = documentId,
        imagePath = imagePath,
        thumbnailPath = thumbnailPath,
        pageOrder = pageOrder,
        createdAt = createdAt
    )
}
