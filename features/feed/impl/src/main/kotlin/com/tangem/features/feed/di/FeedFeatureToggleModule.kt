package com.tangem.features.feed.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.feed.entry.featuretoggle.FeedFeatureToggle
import com.tangem.features.feed.featuretoggle.DefaultFeedFeatureToggle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedFeatureToggleModule {

    @Provides
    @Singleton
    fun provideFeedFeatureToggle(featureTogglesManager: FeatureTogglesManager): FeedFeatureToggle {
        return DefaultFeedFeatureToggle(
            featureTogglesManager = featureTogglesManager,
        )
    }
}