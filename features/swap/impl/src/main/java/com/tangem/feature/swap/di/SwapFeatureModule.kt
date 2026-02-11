package com.tangem.feature.swap.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.feature.swap.DefaultSwapComponent
import com.tangem.feature.swap.DefaultSwapFeatureToggles
import com.tangem.features.swap.SwapComponent
import com.tangem.features.swap.SwapFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SwapFeatureModule {

    @Provides
    @Singleton
    fun provideSwapFeatureToggles(featureTogglesManager: FeatureTogglesManager): SwapFeatureToggles {
        return DefaultSwapFeatureToggles(featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapFeatureModuleBinds {

    @Binds
    @Singleton
    fun provideSwapComponentFactory(impl: DefaultSwapComponent.Factory): SwapComponent.Factory
}