package com.tangem.data.tokens.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.repository.*
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.network.NetworksStatusesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.quote.QuotesStore
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
        appPreferencesStore: AppPreferencesStore,
        userWalletsStore: UserWalletsStore,
        walletManagersFacade: WalletManagersFacade,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
        expressServiceLoader: ExpressServiceLoader,
        excludedBlockchains: ExcludedBlockchains,
    ): CurrenciesRepository {
        return DefaultCurrenciesRepository(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            walletManagersFacade = walletManagersFacade,
            cacheRegistry = cacheRegistry,
            appPreferencesStore = appPreferencesStore,
            expressServiceLoader = expressServiceLoader,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
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
        appPreferencesStore: AppPreferencesStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): NetworksRepository {
        return DefaultNetworksRepository(
            networksStatusesStore = networksStatusesStore,
            walletManagersFacade = walletManagersFacade,
            userWalletsStore = userWalletsStore,
            appPreferencesStore = appPreferencesStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    fun provideCurrencyChecksRepository(
        walletManagersFacade: WalletManagersFacade,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): CurrencyChecksRepository {
        return DefaultCurrencyChecksRepository(
            walletManagersFacade = walletManagersFacade,
            coroutineDispatchers = coroutineDispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun providePolkadotAccountHealthCheckRepository(
        walletManagersFacade: WalletManagersFacade,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): PolkadotAccountHealthCheckRepository {
        return DefaultPolkadotAccountHealthCheckRepository(
            walletManagersFacade = walletManagersFacade,
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }
}