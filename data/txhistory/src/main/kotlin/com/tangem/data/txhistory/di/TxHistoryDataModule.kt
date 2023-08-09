package com.tangem.data.txhistory.di

import com.tangem.data.txhistory.repository.DefaultTxHistoryRepository
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
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
        walletManagersFacade: WalletManagersFacade,
        userWalletsStore: UserWalletsStore,
    ): TxHistoryRepository = DefaultTxHistoryRepository(
        walletManagersFacade = walletManagersFacade,
        userWalletsStore = userWalletsStore,
    )
}