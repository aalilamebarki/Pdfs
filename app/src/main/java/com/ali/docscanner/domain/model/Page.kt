package com.ali.docscanner.domain.model

data class Page(
    val id: Long,
    val documentId: Long,
    val imagePath: String,
    val thumbnailPath: String?,
    val pageOrder: Int,
    val createdAt: Long
)
