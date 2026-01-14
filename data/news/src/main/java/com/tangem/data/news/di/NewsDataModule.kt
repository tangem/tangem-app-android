package com.tangem.data.news.di

import com.tangem.data.news.DefaultNewsErrorResolver
import com.tangem.data.news.repository.DefaultNewsRepository
import com.tangem.datasource.api.news.NewsApi
import com.tangem.datasource.local.news.details.NewsDetailsStore
import com.tangem.datasource.local.news.trending.TrendingNewsStore
import com.tangem.datasource.local.news.viewed.NewsViewedStore
import com.tangem.domain.news.NewsErrorResolver
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
        trendingNewsStore: TrendingNewsStore,
        newsViewedStore: NewsViewedStore,
        newsErrorResolver: NewsErrorResolver,
    ): NewsRepository {
        return DefaultNewsRepository(
            newsApi = newsApi,
            dispatchers = dispatchers,
            newsDetailsStore = newsDetailsStore,
            trendingNewsStore = trendingNewsStore,
            newsViewedStore = newsViewedStore,
            newsErrorResolver = newsErrorResolver,
        )
    }

    @Provides
    fun provideNewsErrorResolver(): NewsErrorResolver = DefaultNewsErrorResolver()
}