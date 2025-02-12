package com.tangem.tap.di.domain

import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object TxHistoryDomainModule {

    @Provides
    fun provideGetTxHistoryItemsCountUseCase(txHistoryRepository: TxHistoryRepository): GetTxHistoryItemsCountUseCase {
        return GetTxHistoryItemsCountUseCase(repository = txHistoryRepository)
    }

    @Provides
    fun provideGetTxHistoryItemsUseCase(txHistoryRepository: TxHistoryRepository): GetTxHistoryItemsUseCase {
        return GetTxHistoryItemsUseCase(repository = txHistoryRepository)
    }

    @Provides
    fun providesGetExplorerTransactionUrlUseCase(
        txHistoryRepository: TxHistoryRepository,
    ): GetExplorerTransactionUrlUseCase {
        return GetExplorerTransactionUrlUseCase(repository = txHistoryRepository)
    }

    @Provides
    fun providesGetFixedTxHistoryItemsUseCase(txHistoryRepository: TxHistoryRepository): GetFixedTxHistoryItemsUseCase {
        return GetFixedTxHistoryItemsUseCase(repository = txHistoryRepository)
    }
}