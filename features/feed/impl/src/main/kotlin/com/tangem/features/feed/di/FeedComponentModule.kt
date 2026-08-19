package com.tangem.features.feed.di

import com.tangem.features.feed.components.DefaultFeedEntryComponent
import com.tangem.features.feed.components.v2.DefaultFeedV2Component
import com.tangem.features.feed.entry.components.FeedEntryComponent
import com.tangem.features.feed.v2.FeedV2Component
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedComponentModule {

    @Binds
    @Singleton
    fun bindFeedEntryComponent(factory: DefaultFeedEntryComponent.Factory): FeedEntryComponent.Factory

    @Binds
    @Singleton
    fun bindFeedV2Component(factory: DefaultFeedV2Component.Factory): FeedV2Component.Factory
}