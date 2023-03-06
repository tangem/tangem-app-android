package com.tangem.tap.proxy.di

import com.tangem.core.analytics.api.AnalyticsEventHandler
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
        )
    }

    @Provides
    @Singleton
    fun provideTransactionManager(
        appStateHolder: AppStateHolder,
        analytics: AnalyticsEventHandler,
    ): TransactionManager {
        return TransactionManagerImpl(appStateHolder, analytics)
    }

    @Provides
    @Singleton
    fun provideDerivationManager(appStateHolder: AppStateHolder): DerivationManager {
        return DerivationManagerImpl(
            appStateHolder = appStateHolder,
        )
    }
}