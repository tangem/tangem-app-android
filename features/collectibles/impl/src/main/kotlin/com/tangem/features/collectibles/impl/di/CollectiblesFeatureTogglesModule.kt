package com.tangem.features.collectibles.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.collectibles.impl.DefaultCollectiblesFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CollectiblesFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideCollectiblesFeatureToggles(featureTogglesManager: FeatureTogglesManager): CollectiblesFeatureToggles {
        return DefaultCollectiblesFeatureToggles(featureTogglesManager)
    }
}