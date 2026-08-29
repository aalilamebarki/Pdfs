package com.ali.docscanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ali.docscanner.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Insert
    suspend fun insert(page: PageEntity): Long

    @Insert
    suspend fun insertAll(pages: List<PageEntity>)

    @Update
    suspend fun update(page: PageEntity)

    @Delete
    suspend fun delete(page: PageEntity)

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageOrder ASC")
    fun observePagesForDocument(documentId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageOrder ASC")
    suspend fun getPagesForDocument(documentId: Long): List<PageEntity>
}
