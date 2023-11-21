package com.tangem.tap.proxy.di

import com.tangem.common.Provider
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.learn2earn.domain.api.Learn2earnDependencyProvider
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.tap.proxy.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    // regions FeatureConsumers
    @Provides
    @Singleton
    fun provideLear2earnDependencies(appStateHolder: AppStateHolder): Learn2earnDependencyProvider {
        return object : Learn2earnDependencyProvider {

            @OptIn(ExperimentalCoroutinesApi::class)
            override fun getCardTypeResolverFlow(): Flow<CardTypesResolver?> {
                return appStateHolder.userWalletListManagerFlow
                    .flatMapLatest { manager ->
                        manager?.selectedUserWallet
                            ?.map { it.scanResponse.cardTypesResolver }
                            ?: flowOf(null)
                    }
            }

            override fun getWebViewAuthCredentialsProvider(): Provider<String?> = Provider {
                appStateHolder.mainStore?.state?.globalState?.configManager?.config?.tangemComAuthorization
            }
        }
    }
    // endregion FeatureConsumers
}