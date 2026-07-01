package com.tangem.lib.auth.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.DefaultAuthFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AuthFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideAuthFeatureToggles(featureTogglesManager: FeatureTogglesManager): AuthFeatureToggles {
        return DefaultAuthFeatureToggles(featureTogglesManager)
    }
}