package com.tangem.data.news.di

import com.tangem.data.news.repository.DefaultNewsRepository
import com.tangem.datasource.api.news.NewsApi
import com.tangem.domain.news.repository.NewsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    fun provideNewsRepository(newsApi: NewsApi, dispatchers: CoroutineDispatcherProvider): NewsRepository {
        return DefaultNewsRepository(
            newsApi = newsApi,
            dispatchers = dispatchers,
        )
    }
}