package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.txhistory.DefaultTxHistoryItemsStore
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.visa.DefaultTangemPayTxHistoryItemsStore
import com.tangem.datasource.local.visa.TangemPayTxHistoryItemsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TxHistoryItemsStoreModule {

    @Provides
    @Singleton
    fun provideTxHistoryItemsStore(): TxHistoryItemsStore {
        return DefaultTxHistoryItemsStore(
            dataStore = RuntimeDataStore(),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayTxHistoryItemsStore(): TangemPayTxHistoryItemsStore {
        return DefaultTangemPayTxHistoryItemsStore(
            dataStore = RuntimeDataStore(),
        )
    }
}