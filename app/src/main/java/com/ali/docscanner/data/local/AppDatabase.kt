package com.ali.docscanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ali.docscanner.data.local.dao.DocumentDao
import com.ali.docscanner.data.local.dao.PageDao
import com.ali.docscanner.data.local.entity.DocumentEntity
import com.ali.docscanner.data.local.entity.PageEntity

@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
}
