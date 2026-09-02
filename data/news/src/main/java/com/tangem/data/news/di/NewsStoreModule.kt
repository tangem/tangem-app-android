package com.tangem.data.news.di

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.news.store.details.DefaultNewsDetailsStore
import com.tangem.data.news.store.details.NewsDetailsStore
import com.tangem.data.news.store.liked.DefaultNewsLikedStore
import com.tangem.data.news.store.liked.NewsLikedStore
import com.tangem.data.news.store.trending.DefaultTrendingNewsStore
import com.tangem.data.news.store.trending.TrendingNewsStore
import com.tangem.data.news.store.viewed.DefaultNewsViewedStore
import com.tangem.data.news.store.viewed.NewsViewedStore
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

    @Provides
    @Singleton
    fun provideNewsViewedStore(): NewsViewedStore {
        return DefaultNewsViewedStore(store = RuntimeSharedStore())
    }

    @Provides
    @Singleton
    fun provideNewsLikedStore(): NewsLikedStore {
        return DefaultNewsLikedStore(store = RuntimeSharedStore())
    }
}