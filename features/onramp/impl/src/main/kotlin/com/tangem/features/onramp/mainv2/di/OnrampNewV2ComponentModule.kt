package com.tangem.features.onramp.mainv2.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.onramp.mainv2.DefaultOnrampV2MainComponent
import com.tangem.features.onramp.mainv2.DefaultOnrampV2MainFeatureToggle
import com.tangem.features.onramp.mainv2.OnrampV2MainComponent
import com.tangem.features.onramp.mainv2.OnrampV2MainFeatureToggle
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampNewMainComponentModule {

    @Binds
    @Singleton
    fun bindOnrampV2MainComponentFactory(factory: DefaultOnrampV2MainComponent.Factory): OnrampV2MainComponent.Factory
}

@Module
@InstallIn(SingletonComponent::class)
internal object FeatureToggleModule {

    @Provides
    @Singleton
    fun provideOnrampV2MainFeatureToggle(featureTogglesManager: FeatureTogglesManager): OnrampV2MainFeatureToggle {
        return DefaultOnrampV2MainFeatureToggle(featureTogglesManager = featureTogglesManager)
    }
}