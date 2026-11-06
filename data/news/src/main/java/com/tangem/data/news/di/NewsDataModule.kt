package com.tangem.data.news.di

import com.tangem.data.news.repository.DefaultNewsRepository
import com.tangem.store.datasource.news.NewsApi
import com.tangem.data.news.store.details.NewsDetailsStore
import com.tangem.data.news.store.liked.NewsLikedStore
import com.tangem.data.news.store.viewed.NewsViewedStore
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
    fun provideNewsRepository(
        newsApi: NewsApi,
        dispatchers: CoroutineDispatcherProvider,
        newsDetailsStore: NewsDetailsStore,
        newsViewedStore: NewsViewedStore,
        newsLikedStore: NewsLikedStore,
    ): NewsRepository {
        return DefaultNewsRepository(
            newsApi = newsApi,
            dispatchers = dispatchers,
            newsDetailsStore = newsDetailsStore,
            newsViewedStore = newsViewedStore,
            newsLikedStore = newsLikedStore,
        )
    }
}