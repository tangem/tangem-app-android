package com.tangem.tap.proxy.di

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.tap.proxy.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.*
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
        appStateHolder: AppStateHolder,
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        userWalletsStore: UserWalletsStore,
    ): UserWalletManager {
        return UserWalletManagerImpl(
            appStateHolder = appStateHolder,
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            userWalletsStore = userWalletsStore,
        )
    }

    @Provides
    @Singleton
    fun provideTransactionManager(
        appStateHolder: AppStateHolder,
        analytics: AnalyticsEventHandler,
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
    ): TransactionManager {
        return TransactionManagerImpl(
            appStateHolder = appStateHolder,
            analytics = analytics,
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun provideDerivationManager(
        appStateHolder: AppStateHolder,
        currenciesRepository: CurrenciesRepository,
        networksRepository: NetworksRepository,
    ): DerivationManager {
        return DerivationManagerImpl(
            appStateHolder = appStateHolder,
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
        )
    }
}