package com.tangem.data.tokens.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.tokens.repository.*
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.TokenReceiveWarningActionStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.express.ExpressServiceFetcher
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
        userTokensResponseStore: UserTokensResponseStore,
        userWalletsStore: UserWalletsStore,
        walletManagersFacade: WalletManagersFacade,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
        expressServiceFetcher: ExpressServiceFetcher,
        excludedBlockchains: ExcludedBlockchains,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        tokensSaver: UserTokensSaver,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    ): CurrenciesRepository {
        return DefaultCurrenciesRepository(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            walletManagersFacade = walletManagersFacade,
            cacheRegistry = cacheRegistry,
            userTokensResponseStore = userTokensResponseStore,
            expressServiceFetcher = expressServiceFetcher,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
            userTokensSaver = tokensSaver,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
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
    fun provideTokenReceiveWarningsViewedRepository(
        tokenReceiveWarningActionStore: TokenReceiveWarningActionStore,
    ): TokenReceiveWarningsViewedRepository {
        return DefaultTokenReceiveWarningsViewedRepository(
            tokenReceiveWarningActionStore = tokenReceiveWarningActionStore,
        )
    }

    @Provides
    @Singleton
    fun provideDefaultYieldSupplyWarningsViewedRepository(
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldSupplyWarningsViewedRepository {
        return DefaultYieldSupplyWarningsViewedRepository(
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }
}