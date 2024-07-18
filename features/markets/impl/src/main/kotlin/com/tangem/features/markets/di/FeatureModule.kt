package com.tangem.features.markets.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.markets.DefaultMarketsFeatureToggles
import com.tangem.features.markets.MarketsFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeatureModule {

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): MarketsFeatureToggles =
        DefaultMarketsFeatureToggles(featureTogglesManager = featureTogglesManager)
}