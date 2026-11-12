package com.tangem.features.feed.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.feed.DefaultFeedFeatureToggles
import com.tangem.features.feed.FeedFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideFeedFeatureToggles(featureTogglesManager: FeatureTogglesManager): FeedFeatureToggles {
        return DefaultFeedFeatureToggles(featureTogglesManager)
    }
}