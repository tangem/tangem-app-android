package com.tangem.features.yield.supply.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.impl.DefaultYieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object YieldSupplyFeatureModule {

    @Singleton
    @Provides
    fun provideYieldFeatureToggles(featureTogglesManager: FeatureTogglesManager): YieldSupplyFeatureToggles {
        return DefaultYieldSupplyFeatureToggles(featureTogglesManager)
    }
}