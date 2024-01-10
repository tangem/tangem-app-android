package com.tangem.feature.tester.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.feature.tester.featuretoggles.DefaultTesterFeatureToggles
import com.tangem.features.tester.api.TesterFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TesterFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideTesterFeatureToggles(featureTogglesManager: FeatureTogglesManager): TesterFeatureToggles {
        return DefaultTesterFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}