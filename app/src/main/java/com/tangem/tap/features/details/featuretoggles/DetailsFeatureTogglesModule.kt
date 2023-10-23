package com.tangem.tap.features.details.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DetailsFeatureTogglesModule {

    @Provides
    fun provideDetailsFeatureToggles(featureTogglesManager: FeatureTogglesManager): DetailsFeatureToggles {
        return DefaultDetailsFeatureToggles(featureTogglesManager)
    }
}
