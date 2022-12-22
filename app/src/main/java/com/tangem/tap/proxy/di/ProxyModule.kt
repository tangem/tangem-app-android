package com.tangem.tap.proxy.di

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.DerivationManagerImpl
import com.tangem.tap.proxy.TransactionManagerImpl
import com.tangem.tap.proxy.UserWalletManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ProxyModule {

    @Provides
    @Singleton
    fun provideAppStateHolder(): AppStateHolder {
        return AppStateHolder()
    }

    @Provides
    @Singleton
    fun provideUserWalletManager(appStateHolder: AppStateHolder): UserWalletManager {
        return UserWalletManagerImpl(
            appStateHolder = appStateHolder,
            walletManagerFactory = WalletManagerFactory(),
        )
    }

    @Provides
    @Singleton
    fun provideTransactionManager(appStateHolder: AppStateHolder): TransactionManager {
        return TransactionManagerImpl(appStateHolder)
    }

    @Provides
    @Singleton
    fun provideDerivationManager(appStateHolder: AppStateHolder): DerivationManager {
        return DerivationManagerImpl(
            appStateHolder = appStateHolder,
        )
    }
}
