package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.swap.DefaultSwapBestRateAnimationStore
import com.tangem.datasource.local.swap.DefaultSwapTransactionStatusStore
import com.tangem.datasource.local.swap.SwapBestRateAnimationStore
import com.tangem.datasource.local.swap.SwapTransactionStatusStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SwapStoreModule {

    @Provides
    @Singleton
    fun provideSwapTransactionStatusStore(): SwapTransactionStatusStore {
        return DefaultSwapTransactionStatusStore(
            dataStore = RuntimeDataStore(),
        )
    }
}