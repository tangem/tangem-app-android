package com.tangem.data.common.cache.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.cache.DefaultCacheRegistry
import com.tangem.datasource.local.cache.CacheKeysStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CacheRegistryModule {

    @Provides
    @Singleton
    fun provideCacheRegistry(cacheKeysStore: CacheKeysStore): CacheRegistry {
        return DefaultCacheRegistry(cacheKeysStore)
    }
}
