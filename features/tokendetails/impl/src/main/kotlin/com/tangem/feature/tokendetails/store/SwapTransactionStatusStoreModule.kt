package com.tangem.feature.tokendetails.store

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SwapTransactionStatusStoreModule {

    @Provides
    @Singleton
    fun provideSwapTransactionStatusStore(): SwapTransactionStatusStore {
        return DefaultSwapTransactionStatusStore(store = RuntimeSharedMapStore())
    }
}