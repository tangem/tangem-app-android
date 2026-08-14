package com.tangem.features.gacha.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.gacha.api.GachaFeatureToggles
import com.tangem.features.gacha.impl.DefaultGachaFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object GachaFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideGachaFeatureToggles(featureTogglesManager: FeatureTogglesManager): GachaFeatureToggles {
        return DefaultGachaFeatureToggles(featureTogglesManager)
    }
}