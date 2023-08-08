package com.tangem.data.txhistory.di

import com.tangem.data.txhistory.repository.DefaultTxHistoryRepository
import com.tangem.domain.txhistory.repository.TxHistoryRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
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
        walletsStateHolder: WalletsStateHolder,
    ): TxHistoryRepository = DefaultTxHistoryRepository(
        walletManagersFacade = walletManagersFacade,
        walletsStateHolder = walletsStateHolder,
    )
}
