package com.tangem.tap.features.details.ui.resetcard.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.details.ui.resetcard.featuretoggles.DefaultResetCardFeatureToggles
import com.tangem.tap.features.details.ui.resetcard.featuretoggles.ResetCardFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ResetCardModule {

    @Provides
    @Singleton
    fun provideResetCardFeatureToggles(featureTogglesManager: FeatureTogglesManager): ResetCardFeatureToggles {
        return DefaultResetCardFeatureToggles(featureTogglesManager)
    }
}