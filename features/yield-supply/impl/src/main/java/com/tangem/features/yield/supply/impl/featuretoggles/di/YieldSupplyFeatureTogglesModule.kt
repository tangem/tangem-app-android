package com.tangem.features.yield.supply.impl.featuretoggles.di

import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.impl.featuretoggles.DefaultYieldSupplyFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyFeatureTogglesModule {

    @Binds
    @Singleton
    fun bindYieldSupplyFeatureToggles(impl: DefaultYieldSupplyFeatureToggles): YieldSupplyFeatureToggles
}