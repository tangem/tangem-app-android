package com.tangem.features.yield.supply.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.impl.DefaultYieldSupplyFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object YieldSupplyFeatureModule {

    @Provides
    @Singleton
    fun provideYieldSupplyFeatureToggles(featureTogglesManager: FeatureTogglesManager): YieldSupplyFeatureToggles {
        return DefaultYieldSupplyFeatureToggles(featureTogglesManager)
    }
}