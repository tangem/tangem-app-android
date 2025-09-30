package com.tangem.data.yield.supply.di

import com.tangem.data.yield.supply.DefaultYieldSupplyMarketRepository
import com.tangem.data.yield.supply.DefaultYieldSupplyErrorResolver
import com.tangem.data.yield.supply.DefaultYieldSupplyTransactionRepository
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
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
        dispatchers: CoroutineDispatcherProvider,
    ): YieldSupplyMarketRepository {
        return DefaultYieldSupplyMarketRepository(
            yieldSupplyApi = yieldSupplyApi,
            store = store,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyErrorResolver(): YieldSupplyErrorResolver {
        return DefaultYieldSupplyErrorResolver
    }
}