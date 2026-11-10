package com.tangem.features.allservices.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.allservices.api.AllServicesFeatureToggles
import com.tangem.features.allservices.impl.DefaultAllServicesFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AllServicesFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideAllServicesFeatureToggles(featureTogglesManager: FeatureTogglesManager): AllServicesFeatureToggles {
        return DefaultAllServicesFeatureToggles(featureTogglesManager)
    }
}