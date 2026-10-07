package com.tangem.data.yield.supply.di

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.yield.supply.DefaultYieldModuleAddressProvider
import com.tangem.data.yield.supply.DefaultYieldSupplyRepository
import com.tangem.data.yield.supply.DefaultYieldSupplyErrorResolver
import com.tangem.data.yield.supply.DefaultYieldSupplyTransactionRepository
import com.tangem.data.yield.supply.promo.DefaultYieldPromoRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.grow.datasource.yield.YieldIncentivesApi
import com.tangem.grow.datasource.yield.YieldSupplyApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.grow.datasource.yield.local.YieldMarketsStore
import com.tangem.data.yield.supply.store.YieldBoostPromoStore
import com.tangem.data.yield.supply.store.YieldBoostStatusStore
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.promo.YieldPromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object YieldSupplyDataModule {

    @Provides
    @Singleton
    fun providerYieldSupplyTransactionRepository(
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldSupplyTransactionRepository {
        return DefaultYieldSupplyTransactionRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGaslessYieldRepository(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    ): GaslessYieldRepository {
        return yieldSupplyTransactionRepository
    }

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun provideYieldSupplyMarketRepository(
        yieldSupplyApi: YieldSupplyApi,
        yieldIncentivesApi: YieldIncentivesApi,
        store: YieldMarketsStore,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        analyticsExceptionHandler: AnalyticsExceptionHandler,
        appPreferencesStore: AppPreferencesStore,
        growPreferencesKeys: GrowPreferencesKeys,
        featureTogglesManager: FeatureTogglesManager,
    ): YieldSupplyRepository {
        return DefaultYieldSupplyRepository(
            yieldSupplyApi = yieldSupplyApi,
            yieldIncentivesApi = yieldIncentivesApi,
            store = store,
            dispatchers = dispatchers,
            walletManagersFacade = walletManagersFacade,
            analyticsExceptionHandler = analyticsExceptionHandler,
            appPreferencesStore = appPreferencesStore,
            growPreferencesKeys = growPreferencesKeys,
            isMerklIncentivesEnabled = featureTogglesManager.isFeatureEnabled(
                FeatureToggles.TWI_1520_MERKL_INCENTIVES_ENABLED,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyErrorResolver(): YieldSupplyErrorResolver {
        return DefaultYieldSupplyErrorResolver
    }

    @Provides
    @Singleton
    fun provideYieldModuleAddressProvider(
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldModuleAddressProvider {
        return DefaultYieldModuleAddressProvider(
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideYieldPromoRepository(
        tangemApi: TangemTechApi,
        promotionsSupplier: PromotionsSupplier,
        promoStore: YieldBoostPromoStore,
        statusStore: YieldBoostStatusStore,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldPromoRepository {
        return DefaultYieldPromoRepository(
            tangemApi = tangemApi,
            promotionsSupplier = promotionsSupplier,
            promoStore = promoStore,
            statusStore = statusStore,
            dispatchers = dispatchers,
        )
    }
}