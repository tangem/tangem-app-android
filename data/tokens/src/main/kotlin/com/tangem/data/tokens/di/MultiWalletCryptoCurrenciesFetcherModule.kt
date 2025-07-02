package com.tangem.data.tokens.di

import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.tokens.DefaultMultiWalletCryptoCurrenciesFetcher
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class MultiWalletCryptoCurrenciesFetcherModule {

    @Singleton
    @Provides
    fun provideMultiWalletCryptoCurrenciesFetcher(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        userTokensResponseStore: UserTokensResponseStore,
        userTokensSaver: UserTokensSaver,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        expressServiceLoader: ExpressServiceLoader,
        dispatchers: CoroutineDispatcherProvider,
    ): MultiWalletCryptoCurrenciesFetcher {
        return DefaultMultiWalletCryptoCurrenciesFetcher(
            demoConfig = DemoConfig(),
            userWalletsStore = userWalletsStore,
            tangemTechApi = tangemTechApi,
            customTokensMerger = CustomTokensMerger(
                tangemTechApi = tangemTechApi,
                userTokensSaver = userTokensSaver,
                dispatchers = dispatchers,
            ),
            userTokensResponseStore = userTokensResponseStore,
            userTokensSaver = userTokensSaver,
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
            expressServiceLoader = expressServiceLoader,
            dispatchers = dispatchers,
        )
    }
}