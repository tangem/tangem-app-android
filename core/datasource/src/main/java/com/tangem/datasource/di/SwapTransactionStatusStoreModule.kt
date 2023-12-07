package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.swaptx.DefaultSwapTransactionStatusStore
import com.tangem.datasource.local.swaptx.SwapTransactionStatusStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SwapTransactionStatusStoreModule {

    @Provides
    @Singleton
    fun provideSwapTransactionStatusStore(): SwapTransactionStatusStore {
        return DefaultSwapTransactionStatusStore(
            dataStore = RuntimeDataStore(),
        )
    }
}