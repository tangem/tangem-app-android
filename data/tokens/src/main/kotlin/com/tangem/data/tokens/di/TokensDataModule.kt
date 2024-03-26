package com.tangem.data.tokens.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.repository.*
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.network.NetworksStatusesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.datasource.local.token.AssetsStore
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.repository.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokensDataModule {

    @Provides
    @Singleton
    fun provideCurrenciesRepository(
        tangemTechApi: TangemTechApi,
        tangemExpressApi: TangemExpressApi,
        userTokensStore: UserTokensStore,
        userWalletsStore: UserWalletsStore,
        walletManagersFacade: WalletManagersFacade,
        assetsStore: AssetsStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): CurrenciesRepository {
        return DefaultCurrenciesRepository(
            tangemTechApi = tangemTechApi,
            tangemExpressApi = tangemExpressApi,
            userTokensStore = userTokensStore,
            walletManagersFacade = walletManagersFacade,
            userWalletsStore = userWalletsStore,
            assetsStore = assetsStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideQuotesRepository(
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        quotesStore: QuotesStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): QuotesRepository {
        return DefaultQuotesRepository(
            tangemTechApi = tangemTechApi,
            appPreferencesStore = appPreferencesStore,
            quotesStore = quotesStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideNetworksRepository(
        networksStatusesStore: NetworksStatusesStore,
        walletManagersFacade: WalletManagersFacade,
        userWalletsStore: UserWalletsStore,
        userTokensStore: UserTokensStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): NetworksRepository {
        return DefaultNetworksRepository(
            networksStatusesStore = networksStatusesStore,
            walletManagersFacade = walletManagersFacade,
            userWalletsStore = userWalletsStore,
            userTokensStore = userTokensStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideDefaultMarketCoinsRepository(assetsStore: AssetsStore): MarketCryptoCurrencyRepository {
        return DefaultMarketCryptoCurrencyRepository(assetsStore)
    }

    @Provides
    @Singleton
    fun providesTokensListRepository(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
        quotesRepository: QuotesRepository,
    ): TokensListRepository {
        return DefaultTokensListRepository(
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
            quotesRepository = quotesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideCardNetworksRepository(
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): NetworksCompatibilityRepository {
        return DefaultNetworksCompatibilityRepository(userWalletsStore = userWalletsStore, dispatchers = dispatchers)
    }

    @Provides
    @Singleton
    fun provideCurrencyChecksRepository(walletManagersFacade: WalletManagersFacade): CurrencyChecksRepository {
        return DefaultCurrencyChecksRepository(walletManagersFacade = walletManagersFacade)
    }

    @Provides
    @Singleton
    fun providePolkadotAccountHealthCheckRepository(
        walletManagersFacade: WalletManagersFacade,
        appPreferencesStore: AppPreferencesStore,
    ): PolkadotAccountHealthCheckRepository {
        return DefaultPolkadotAccountHealthCheckRepository(
            walletManagersFacade = walletManagersFacade,
            appPreferencesStore = appPreferencesStore,
        )
    }
}