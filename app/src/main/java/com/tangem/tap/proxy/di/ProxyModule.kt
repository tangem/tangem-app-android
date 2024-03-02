package com.tangem.tap.proxy.di

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
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
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        userWalletsStore: UserWalletsStore,
    ): UserWalletManager {
        return UserWalletManagerImpl(
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            userWalletsStore = userWalletsStore,
        )
    }

    @Provides
    @Singleton
    fun provideTransactionManager(
        appStateHolder: AppStateHolder,
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
    ): TransactionManager {
        return TransactionManagerImpl(
            appStateHolder = appStateHolder,
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
        )
    }
}