package com.tangem.data.common.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.cache.etag.DefaultETagsStore
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.*
import com.tangem.data.common.quote.DefaultQuotesFetcher
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.data.common.wallet.DefaultWalletServerBinder
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.retryer.RetryerPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataCommonModule {

    @Provides
    @Singleton
    fun provideCardCryptoCurrencyFactory(
        excludedBlockchains: ExcludedBlockchains,
        userWalletsStore: UserWalletsStore,
        accountsFeatureToggles: AccountsFeatureToggles,
        walletAccountsFetcher: WalletAccountsFetcher,
        userTokensResponseStore: UserTokensResponseStore,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    ): CardCryptoCurrencyFactory {
        return DefaultCardCryptoCurrencyFactory(
            demoConfig = DemoConfig,
            excludedBlockchains = excludedBlockchains,
            userWalletsStore = userWalletsStore,
            accountsFeatureToggles = accountsFeatureToggles,
            walletAccountsFetcher = walletAccountsFetcher,
            userTokensResponseStore = userTokensResponseStore,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        )
    }

    @Provides
    @Singleton
    fun provideUserTokensEncricher(
        walletsRepository: WalletsRepository,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): UserTokensResponseAddressesEnricher {
        return UserTokensResponseAddressesEnricher(
            walletsRepository = walletsRepository,
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideUserTokensSaver(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        userTokensResponseStore: UserTokensResponseStore,
        dispatchers: CoroutineDispatcherProvider,
        addressesEnricher: UserTokensResponseAddressesEnricher,
        walletServerBinder: WalletServerBinder,
        appsFlyerConversionStore: AppsFlyerConversionStore,
        accountsFeatureToggles: AccountsFeatureToggles,
    ): UserTokensSaver {
        return UserTokensSaver(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            userTokensResponseStore = userTokensResponseStore,
            dispatchers = dispatchers,
            addressesEnricher = addressesEnricher,
            accountsFeatureToggles = accountsFeatureToggles,
            pushTokensRetryerPool = RetryerPool(
                coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default),
            ),
            walletServerBinder = walletServerBinder,
            appsFlyerConversionStore = appsFlyerConversionStore,
        )
    }

    @Provides
    @Singleton
    fun provideQuotesFetcher(tangemTechApi: TangemTechApi, dispatchers: CoroutineDispatcherProvider): QuotesFetcher {
        return DefaultQuotesFetcher(tangemTechApi = tangemTechApi, dispatchers = dispatchers)
    }

    @Provides
    @Singleton
    fun provideETagsStore(appPreferencesStore: AppPreferencesStore): ETagsStore {
        return DefaultETagsStore(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun provideWalletServerBinder(
        userWalletsStore: UserWalletsStore,
        appsFlyerConversionStore: AppsFlyerConversionStore,
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletServerBinder {
        return DefaultWalletServerBinder(
            userWalletsStore = userWalletsStore,
            appsFlyerConversionStore = appsFlyerConversionStore,
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
        )
    }
}