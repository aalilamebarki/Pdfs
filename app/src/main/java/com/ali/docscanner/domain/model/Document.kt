package com.ali.docscanner.domain.model

data class Document(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnailPath: String?,
    val pageCount: Int
)
