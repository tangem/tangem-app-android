package com.tangem.features.yieldlending.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yieldlending.api.YieldLendingFeatureToggles
import com.tangem.features.yieldlending.impl.DefaultYieldLendingFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object YieldLendingFeatureModule {

    @Singleton
    @Provides
    fun provideYieldFeatureToggles(featureTogglesManager: FeatureTogglesManager): YieldLendingFeatureToggles {
        return DefaultYieldLendingFeatureToggles(featureTogglesManager)
    }
}