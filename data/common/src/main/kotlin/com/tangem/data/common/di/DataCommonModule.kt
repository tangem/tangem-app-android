package com.tangem.data.common.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.cache.etag.DefaultETagsStore
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.*
import com.tangem.data.common.quote.DefaultQuotesFetcher
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataCommonModule {

    @Provides
    @Singleton
    fun provideCardCryptoCurrencyFactory(
        excludedBlockchains: ExcludedBlockchains,
        userWalletsStore: UserWalletsStore,
        userTokensResponseStore: UserTokensResponseStore,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    ): CardCryptoCurrencyFactory {
        return DefaultCardCryptoCurrencyFactory(
            demoConfig = DemoConfig(),
            excludedBlockchains = excludedBlockchains,
            userWalletsStore = userWalletsStore,
            userTokensResponseStore = userTokensResponseStore,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        )
    }

    @Provides
    @Singleton
    fun provideUserTokensEncricher(
        walletsRepository: WalletsRepository,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        dispatchers: CoroutineDispatcherProvider,
    ): UserTokensResponseAddressesEnricher {
        return UserTokensResponseAddressesEnricher(
            walletsRepository = walletsRepository,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideUserTokensSaver(
        tangemTechApi: TangemTechApi,
        userTokensResponseStore: UserTokensResponseStore,
        dispatchers: CoroutineDispatcherProvider,
        addressesEnricher: UserTokensResponseAddressesEnricher,
        accountsFeatureToggles: AccountsFeatureToggles,
    ): UserTokensSaver {
        return UserTokensSaver(
            tangemTechApi = tangemTechApi,
            userTokensResponseStore = userTokensResponseStore,
            dispatchers = dispatchers,
            addressesEnricher = addressesEnricher,
            accountsFeatureToggles = accountsFeatureToggles,
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
}