package com.tangem.datasource.di

import com.tangem.datasource.local.cache.CacheKeysStore
import com.tangem.datasource.local.cache.DefaultCacheKeysStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
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
        return DefaultCacheKeysStore(
            dataStore = RuntimeDataStore(),
        )
    }
}