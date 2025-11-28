package com.tangem.data.tokens.di

import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.tokens.AccountListCryptoCurrenciesFetcher
import com.tangem.data.tokens.DefaultMultiWalletCryptoCurrenciesFetcher
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.express.ExpressServiceFetcher
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
        accountsFeatureToggles: AccountsFeatureToggles,
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        userTokensResponseStore: UserTokensResponseStore,
        userTokensSaver: UserTokensSaver,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        expressServiceFetcher: ExpressServiceFetcher,
        walletAccountsFetcher: WalletAccountsFetcher,
        dispatchers: CoroutineDispatcherProvider,
    ): MultiWalletCryptoCurrenciesFetcher {
        return if (accountsFeatureToggles.isFeatureEnabled) {
            AccountListCryptoCurrenciesFetcher(
                userWalletsStore = userWalletsStore,
                walletAccountsFetcher = walletAccountsFetcher,
                expressServiceFetcher = expressServiceFetcher,
                dispatchers = dispatchers,
            )
        } else {
            DefaultMultiWalletCryptoCurrenciesFetcher(
                demoConfig = DemoConfig,
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
                expressServiceFetcher = expressServiceFetcher,
                dispatchers = dispatchers,
            )
        }
    }
}