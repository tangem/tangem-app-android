package com.tangem.features.feed.di

import com.tangem.features.feed.components.DefaultFeedEntryComponent
import com.tangem.features.feed.entry.components.FeedEntryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindFeedEntryComponent(factory: DefaultFeedEntryComponent.Factory): FeedEntryComponent.Factory
}