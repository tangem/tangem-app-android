package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.txhistory.DefaultTxHistoryItemsStore
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object TxHistoryItemsStoreModule {

    @Provides
    fun provideTxHistoryItemsStore(): TxHistoryItemsStore {
        return DefaultTxHistoryItemsStore(
            dataStore = RuntimeDataStore(),
        )
    }
}
