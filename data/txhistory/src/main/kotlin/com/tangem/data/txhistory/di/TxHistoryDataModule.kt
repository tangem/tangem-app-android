package com.tangem.data.txhistory.di

import com.tangem.data.common.txhistory.ExpressHistoryRepository
import com.tangem.data.txhistory.fetcher.DefaultAppTxHistoryFetcher
import com.tangem.data.txhistory.fetcher.DefaultTxHistoryFetcherUtils
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils
import com.tangem.data.txhistory.repository.DefaultExpressHistoryRepository
import com.tangem.data.txhistory.repository.DefaultTxHistoryRepository
import com.tangem.data.txhistory.repository.RefactoredTxHistoryRepository
import com.tangem.domain.txhistory.fetcher.AppTxHistoryFetcher
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TxHistoryDataModule {

    @Binds
    @Singleton
    fun provideTxHistoryRepository(default: DefaultTxHistoryRepository): TxHistoryRepository

    @Binds
    @Singleton
    fun provideTxHistoryRepositoryV2(default: RefactoredTxHistoryRepository): TxHistoryRepositoryV2

    @Binds
    @Singleton
    fun provideAppTxHistoryFetcher(default: DefaultAppTxHistoryFetcher): AppTxHistoryFetcher

    @Binds
    fun provideTxHistoryFetcherUtils(default: DefaultTxHistoryFetcherUtils): TxHistoryFetcherUtils

    @Binds
    @Singleton
    fun provideExpressHistoryRepository(default: DefaultExpressHistoryRepository): ExpressHistoryRepository
}