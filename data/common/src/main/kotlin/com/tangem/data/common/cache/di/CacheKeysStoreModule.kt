package com.tangem.data.common.cache.di

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.data.common.cache.CacheKeysStore
import com.tangem.data.common.cache.DefaultCacheKeysStore
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
            store = RuntimeSharedMapStore(),
        )
    }
}