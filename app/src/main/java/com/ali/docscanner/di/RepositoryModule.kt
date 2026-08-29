package com.ali.docscanner.di

import com.ali.docscanner.data.repository.DocumentRepository
import com.ali.docscanner.data.repository.DocumentRepositoryImpl
import com.ali.docscanner.data.repository.PageRepository
import com.ali.docscanner.data.repository.PageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindPageRepository(
        impl: PageRepositoryImpl
    ): PageRepository
}
