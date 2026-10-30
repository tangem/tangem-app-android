package com.tangem.feature.usedesk.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.feature.usedesk.DefaultUseDeskFeatureToggles
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UseDeskFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideUseDeskFeatureToggles(featureTogglesManager: FeatureTogglesManager): UseDeskFeatureToggles {
        return DefaultUseDeskFeatureToggles(featureTogglesManager)
    }
}