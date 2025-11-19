package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.news.details.DefaultNewsDetailsStore
import com.tangem.datasource.local.news.details.NewsDetailsStore
import com.tangem.datasource.local.news.trending.DefaultTrendingNewsStore
import com.tangem.datasource.local.news.trending.TrendingNewsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NewsStoreModule {

    @Provides
    @Singleton
    fun provideNewsDetailsStore(): NewsDetailsStore {
        return DefaultNewsDetailsStore(store = RuntimeSharedStore())
    }

    @Provides
    @Singleton
    fun provideTrendingNewsStore(): TrendingNewsStore {
        return DefaultTrendingNewsStore(store = RuntimeSharedStore())
    }
}