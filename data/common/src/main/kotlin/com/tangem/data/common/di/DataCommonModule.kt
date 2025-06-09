package com.tangem.data.common.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.DefaultCardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseAddressesEnricher
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
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
    ): CardCryptoCurrencyFactory {
        return DefaultCardCryptoCurrencyFactory(
            demoConfig = DemoConfig(),
            excludedBlockchains = excludedBlockchains,
            userWalletsStore = userWalletsStore,
            userTokensResponseStore = userTokensResponseStore,
        )
    }

    @Provides
    @Singleton
    fun provideUserTokensEncricher(
        notificationsFeatureToggles: NotificationsFeatureToggles,
        walletsRepository: WalletsRepository,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        dispatchers: CoroutineDispatcherProvider,
    ): UserTokensResponseAddressesEnricher {
        return UserTokensResponseAddressesEnricher(
            notificationsFeatureToggles = notificationsFeatureToggles,
            walletsRepository = walletsRepository,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideUserTokensSaver(
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
        enricher: UserTokensResponseAddressesEnricher,
    ): UserTokensSaver {
        return UserTokensSaver(
            tangemTechApi = tangemTechApi,
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
            userTokensResponseAddressesEnricher = enricher,
        )
    }
}