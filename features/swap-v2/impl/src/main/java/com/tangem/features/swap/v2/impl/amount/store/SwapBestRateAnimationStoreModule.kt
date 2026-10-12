package com.tangem.features.swap.v2.impl.amount.store

import com.tangem.core.local.datastore.RuntimeSharedStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SwapBestRateAnimationStoreModule {

    @Provides
    @Singleton
    fun provideSwapBestRateAnimationStore(): SwapBestRateAnimationStore {
        return DefaultSwapBestRateAnimationStore(dataStore = RuntimeSharedStore())
    }
}