package com.tangem.features.feed.di

import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.featuretoggles.DefaultFeedFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedFeatureTogglesModule {

    @Binds
    @Singleton
    fun bindFeedFeatureToggles(impl: DefaultFeedFeatureToggles): FeedFeatureToggles
}