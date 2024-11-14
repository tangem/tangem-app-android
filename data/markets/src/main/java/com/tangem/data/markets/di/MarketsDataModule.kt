package com.tangem.data.markets.di

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.markets.DefaultMarketsTokenRepository
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MarketsDataModule {

    @Provides
    @Singleton
    fun provideMarketsRepository(
        marketsApi: TangemTechMarketsApi,
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
        analyticsEventHandler: AnalyticsEventHandler,
        cacheRegistry: CacheRegistry,
    ): MarketsTokenRepository {
        return DefaultMarketsTokenRepository(
            marketsApi = marketsApi,
            tangemTechApi = tangemTechApi,
            dispatcherProvider = dispatchers,
            userWalletsStore = userWalletsStore,
            analyticsEventHandler = analyticsEventHandler,
            cacheRegistry = cacheRegistry,
            tokenExchangesStore = RuntimeStateStore(defaultValue = emptyList()),
        )
    }
}
