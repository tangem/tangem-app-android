package com.tangem.data.tokens.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.repository.DefaultCurrenciesRepository
import com.tangem.data.tokens.repository.DefaultMarketCryptoCurrencyRepository
import com.tangem.data.tokens.repository.DefaultNetworksRepository
import com.tangem.data.tokens.repository.DefaultQuotesRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.appcurrency.SelectedAppCurrencyStore
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.datasource.local.token.UserMarketCoinsStore
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
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
        userTokensStore: UserTokensStore,
        userWalletsStore: UserWalletsStore,
        userMarketCoinsStore: UserMarketCoinsStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): CurrenciesRepository {
        return DefaultCurrenciesRepository(
            tangemTechApi = tangemTechApi,
            userTokensStore = userTokensStore,
            userWalletsStore = userWalletsStore,
            userMarketCoinsStore = userMarketCoinsStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideQuotesRepository(
        tangemTechApi: TangemTechApi,
        quotesStore: QuotesStore,
        selectedAppCurrencyStore: SelectedAppCurrencyStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): QuotesRepository {
        return DefaultQuotesRepository(
            tangemTechApi = tangemTechApi,
            quotesStore = quotesStore,
            selectedAppCurrencyStore = selectedAppCurrencyStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideNetworksRepository(
        walletManagersFacade: WalletManagersFacade,
        userWalletsStore: UserWalletsStore,
        userTokensStore: UserTokensStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): NetworksRepository {
        return DefaultNetworksRepository(
            walletManagersFacade = walletManagersFacade,
            userWalletsStore = userWalletsStore,
            userTokensStore = userTokensStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideDefaultMarketCoinsRepository(
        userMarketCoinsStore: UserMarketCoinsStore,
    ): MarketCryptoCurrencyRepository {
        return DefaultMarketCryptoCurrencyRepository(userMarketCoinsStore)
    }
}
