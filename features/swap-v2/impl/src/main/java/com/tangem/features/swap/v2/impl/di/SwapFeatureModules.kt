package com.tangem.features.swap.v2.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.v2.api.SwapFeatureToggles
import com.tangem.features.swap.v2.impl.DefaultSwapFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SwapFeatureModules {

    @Provides
    @Singleton
    fun provideSwapFeatureToggles(featureTogglesManager: FeatureTogglesManager): SwapFeatureToggles {
        return DefaultSwapFeatureToggles(featureTogglesManager)
    }
}