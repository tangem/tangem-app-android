package com.tangem.tap.proxy.di

import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.UserWalletManagerImpl
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
        dispatchers: CoroutineDispatcherProvider,
    ): UserWalletManager {
        return UserWalletManagerImpl(
            walletManagersFacade = walletManagersFacade,
            userWalletsListManager = userWalletsListManager,
            dispatchers = dispatchers,
        )
    }
}