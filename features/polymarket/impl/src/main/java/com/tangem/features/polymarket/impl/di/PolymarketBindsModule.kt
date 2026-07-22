package com.tangem.features.polymarket.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.DefaultPolymarketComponent
import com.tangem.features.polymarket.impl.featuretoggles.DefaultPolymarketFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketBindsModule {
    @Binds
    @Singleton
    fun providePolymarketComponentFactory(impl: DefaultPolymarketComponent.Factory): PolymarketComponent.Factory
}

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketFeatureTogglesModule {

    @Provides
    @Singleton
    fun providePolymarketFeatureToggles(featureTogglesManager: FeatureTogglesManager): PolymarketFeatureToggles {
        return DefaultPolymarketFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}