package com.tangem.data.yield.supply.di

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.data.yield.supply.DefaultYieldSupplyRepository
import com.tangem.data.yield.supply.DefaultYieldSupplyErrorResolver
import com.tangem.data.yield.supply.DefaultYieldSupplyTransactionRepository
import com.tangem.data.yield.supply.promo.DefaultYieldPromoRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostPromoStore
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostStatusStore
import com.tangem.domain.walletmanager.WalletManagersFacade
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
    fun provideYieldSupplyMarketRepository(
        yieldSupplyApi: YieldSupplyApi,
        store: YieldMarketsStore,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        analyticsExceptionHandler: AnalyticsExceptionHandler,
        appPreferencesStore: AppPreferencesStore,
    ): YieldSupplyRepository {
        return DefaultYieldSupplyRepository(
            yieldSupplyApi = yieldSupplyApi,
            store = store,
            dispatchers = dispatchers,
            walletManagersFacade = walletManagersFacade,
            analyticsExceptionHandler = analyticsExceptionHandler,
            appPreferencesStore = appPreferencesStore,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyErrorResolver(): YieldSupplyErrorResolver {
        return DefaultYieldSupplyErrorResolver
    }

    @Provides
    @Singleton
    fun provideYieldPromoRepository(
        tangemApi: TangemTechApi,
        promoStore: YieldBoostPromoStore,
        statusStore: YieldBoostStatusStore,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldPromoRepository {
        return DefaultYieldPromoRepository(
            tangemApi = tangemApi,
            promoStore = promoStore,
            statusStore = statusStore,
            dispatchers = dispatchers,
        )
    }
}