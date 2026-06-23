package com.tangem.features.forceupdate.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.forceupdate.ForceUpdateFeatureToggles
import com.tangem.features.forceupdate.impl.DefaultForceUpdateFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ForceUpdateFeatureModule {

    @Provides
    @Singleton
    fun provideForceUpdateFeatureToggles(featureTogglesManager: FeatureTogglesManager): ForceUpdateFeatureToggles =
        DefaultForceUpdateFeatureToggles(featureTogglesManager)
}