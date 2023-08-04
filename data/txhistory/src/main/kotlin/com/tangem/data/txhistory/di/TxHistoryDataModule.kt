package com.tangem.data.txhistory.di

import com.tangem.data.txhistory.repository.MockTxHistoryRepository
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TxHistoryDataModule {

    @Provides
    @Singleton
    fun provideTxHistoryRepository(): TxHistoryRepository = MockTxHistoryRepository()
}
