package com.tangem.features.yield.supply.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.impl.DefaultYieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.impl.common.DefaultYieldSupplyProtocolTrigger
import com.tangem.features.yield.supply.impl.common.YieldSupplyProtocolListener
import com.tangem.features.yield.supply.impl.common.YieldSupplyProtocolTrigger
import dagger.Binds
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

@InstallIn(SingletonComponent::class)
@Module
internal interface YieldSupplyProtocolModuleBinds {

    @Singleton
    @Binds
    fun bindYieldSupplyProtocolTrigger(impl: DefaultYieldSupplyProtocolTrigger): YieldSupplyProtocolTrigger

    @Singleton
    @Binds
    fun bindYieldSupplyProtocolListener(impl: DefaultYieldSupplyProtocolTrigger): YieldSupplyProtocolListener
}