package com.tangem.data.txhistory.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.txhistory.repository.DefaultTxHistoryRepository
import com.tangem.data.txhistory.repository.RefactoredTxHistoryRepository
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    fun provideTxHistoryRepository(
        cacheRegistry: CacheRegistry,
        walletManagersFacade: WalletManagersFacade,
        userWalletsStore: UserWalletsStore,
        txHistoryItemsStore: TxHistoryItemsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): TxHistoryRepository = DefaultTxHistoryRepository(
        cacheRegistry,
        walletManagersFacade,
        userWalletsStore,
        txHistoryItemsStore,
        dispatchers,
    )

    @Provides
    @Singleton
    fun provideTxHistoryRepositoryV2(
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        txHistoryItemsStore: TxHistoryItemsStore,
        cacheRegistry: CacheRegistry,
    ): TxHistoryRepositoryV2 = RefactoredTxHistoryRepository(
        walletManagersFacade = walletManagersFacade,
        dispatchers = dispatchers,
        txHistoryItemsStore = txHistoryItemsStore,
        cacheRegistry = cacheRegistry,
    )
}