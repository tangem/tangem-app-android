package com.tangem.data.yield.supply.di

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.yield.supply.status.DefaultYieldModuleStatusFetcher
import com.tangem.data.yield.supply.status.DefaultYieldModuleStatusProducer
import com.tangem.data.yield.supply.status.YieldModuleStatusDataCleaner
import com.tangem.data.yield.supply.status.store.DefaultYieldModuleStatusStore
import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.yield.supply.YieldSupplyBalanceFetcher
import com.tangem.domain.models.yield.supply.YieldSupplyBalanceProvider
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.status.YieldModuleStatusContributionFetcher
import com.tangem.domain.yield.supply.status.YieldModuleStatusContributionProvider
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object YieldModuleStatusModule {

    @Provides
    @Singleton
    fun provideYieldModuleStatusStore(
        dataStoreFactory: AppDataStoreFactory,
        appScope: AppCoroutineScope,
    ): YieldModuleStatusStore {
        return DefaultYieldModuleStatusStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceStore = dataStoreFactory.create(defaultValue = emptyMap(), fileName = "yield_module_statuses"),
            scope = appScope,
        )
    }

    @Provides
    @Singleton
    fun provideYieldModuleStatusSupplier(
        factory: DefaultYieldModuleStatusProducer.Factory,
    ): YieldModuleStatusSupplier {
        return YieldModuleStatusSupplier(
            factory = factory,
            keyCreator = { "yield_module_status_${it.userWalletId.stringValue}" },
        )
    }

    @Provides
    @Singleton
    fun provideYieldModuleStatusFetcher(impl: DefaultYieldModuleStatusFetcher): YieldModuleStatusFetcher {
        return impl
    }

    @Provides
    fun provideYieldSupplyBalanceFetcher(
        yieldModuleStatusFetcher: YieldModuleStatusFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): YieldSupplyBalanceFetcher {
        return YieldModuleStatusContributionFetcher(
            yieldModuleStatusFetcher = yieldModuleStatusFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @IntoSet
    fun provideYieldSupplyBalanceFetcherIntoSet(fetcher: YieldSupplyBalanceFetcher): BalanceContributionFetcher {
        return fetcher
    }

    @Provides
    fun provideYieldSupplyBalanceProvider(
        yieldModuleStatusSupplier: YieldModuleStatusSupplier,
        tokensFeatureToggles: TokensFeatureToggles,
    ): YieldSupplyBalanceProvider {
        return YieldModuleStatusContributionProvider(
            yieldModuleStatusSupplier = yieldModuleStatusSupplier,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @IntoSet
    fun provideYieldSupplyBalanceProviderIntoSet(provider: YieldSupplyBalanceProvider): BalanceContributionProvider {
        return provider
    }

    @Provides
    @IntoSet
    fun provideYieldModuleStatusDataCleaner(impl: YieldModuleStatusDataCleaner): CryptoCurrencyDataCleaner {
        return impl
    }
}