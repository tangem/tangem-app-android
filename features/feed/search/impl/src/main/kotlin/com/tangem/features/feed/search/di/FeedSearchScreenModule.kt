package com.tangem.features.feed.search.di

import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.search.FeedSearchComponent
import com.tangem.features.feed.search.components.DefaultFeedSearchComponent
import com.tangem.features.feed.search.components.FeedSearchScreenFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedSearchScreenModule {

    @Binds
    fun bindFeedSearchComponentFactory(impl: DefaultFeedSearchComponent.Factory): FeedSearchComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(FeedRoute.Search::class)
    fun bindFeedSearchScreenFactory(impl: FeedSearchScreenFactory): FeedScreenFactory
}