package com.tangem.data.news.di

import com.tangem.data.news.repository.DefaultNewsRepository
import com.tangem.domain.news.repository.NewsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NewsDataModule {

    @Singleton
    @Provides
    fun providesQuotesRepository(): NewsRepository {
        return DefaultNewsRepository()
    }
}