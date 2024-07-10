package com.tangem.tap.proxy.di

import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.TransactionManagerImpl
import com.tangem.tap.proxy.UserWalletManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ProxyModule {

    @Provides
    @Singleton
    fun provideAppStateHolder(): AppStateHolder {
        return AppStateHolder()
    }

    @Provides
    @Singleton
    fun provideUserWalletManager(
        walletManagersFacade: WalletManagersFacade,
        userWalletsListManager: UserWalletsListManager,
    ): UserWalletManager {
        return UserWalletManagerImpl(
            walletManagersFacade = walletManagersFacade,
            userWalletsListManager = userWalletsListManager,
        )
    }

    @Provides
    @Singleton
    fun provideTransactionManager(
        walletManagersFacade: WalletManagersFacade,
        userWalletsListManager: UserWalletsListManager,
    ): TransactionManager {
        return TransactionManagerImpl(
            walletManagersFacade = walletManagersFacade,
            userWalletsListManager = userWalletsListManager,
        )
    }
}