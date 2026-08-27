package com.ali.docscanner.di

import android.content.Context
import androidx.room.Room
import com.ali.docscanner.data.local.AppDatabase
import com.ali.docscanner.data.local.dao.DocumentDao
import com.ali.docscanner.data.local.dao.PageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "docscanner.db"
        ).build()
    }

    @Provides
    fun provideDocumentDao(db: AppDatabase): DocumentDao = db.documentDao()

    @Provides
    fun providePageDao(db: AppDatabase): PageDao = db.pageDao()
}
