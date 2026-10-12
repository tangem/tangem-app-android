package com.tangem.store.datasource.txhistory

import com.tangem.core.local.datastore.RuntimeSharedMapStore
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
            store = RuntimeSharedMapStore(),
        )
    }
}