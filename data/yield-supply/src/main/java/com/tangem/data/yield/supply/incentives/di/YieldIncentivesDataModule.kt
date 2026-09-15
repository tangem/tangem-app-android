package com.tangem.data.yield.supply.incentives.di

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.yield.supply.incentives.DefaultYieldIncentivesRepository
import com.tangem.data.yield.supply.incentives.cache.DefaultYieldIncentivesCache
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.grow.datasource.yield.YieldIncentivesApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object YieldIncentivesDataModule {

    @Provides
    @Singleton
    fun provideYieldIncentivesRepository(
        api: YieldIncentivesApi,
        featureTogglesManager: FeatureTogglesManager,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldIncentivesRepository {
        return DefaultYieldIncentivesRepository(
            api = api,
            cache = DefaultYieldIncentivesCache(),
            dispatchers = dispatchers,
            isMerklIncentivesEnabled = featureTogglesManager.isFeatureEnabled(
                FeatureToggles.TWI_1520_MERKL_INCENTIVES_ENABLED,
            ),
        )
    }
}