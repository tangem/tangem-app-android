package com.tangem.tap.di.domain

import com.tangem.domain.tokens.*
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object TxHistoryDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetTxHistoryItemsCountUseCase(txHistoryRepository: TxHistoryRepository): GetTxHistoryItemsCountUseCase {
        return GetTxHistoryItemsCountUseCase(repository = txHistoryRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetTxHistoryItemsUseCase(txHistoryRepository: TxHistoryRepository): GetTxHistoryItemsUseCase {
        return GetTxHistoryItemsUseCase(repository = txHistoryRepository)
    }
}