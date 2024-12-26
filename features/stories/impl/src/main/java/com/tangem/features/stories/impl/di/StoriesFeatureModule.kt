package com.tangem.features.stories.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.stories.api.StoriesFeatureToggles
import com.tangem.features.stories.impl.DefaultStoriesFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StoriesFeatureModule {

    @Provides
    @Singleton
    fun provideStoriesFeatureToggles(featureTogglesManager: FeatureTogglesManager): StoriesFeatureToggles {
        return DefaultStoriesFeatureToggles(featureTogglesManager)
    }
}