package com.tangem.datasource.di

import com.tangem.datasource.local.cache.CacheKeysStore
import com.tangem.datasource.local.cache.RuntimeCacheKeysStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CacheKeysStoreModule {

    @Provides
    @Singleton
    fun provideCacheKeysStore(): CacheKeysStore {
        return RuntimeCacheKeysStore()
    }
}