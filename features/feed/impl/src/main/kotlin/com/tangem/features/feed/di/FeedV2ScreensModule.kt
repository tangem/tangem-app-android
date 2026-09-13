package com.tangem.features.feed.di

import com.tangem.features.feed.components.v2.FeedForYouScreenFactory
import com.tangem.features.feed.components.v2.FeedMarketsTokenDetailsScreenFactory
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/** Feed's own screens contributed to the v2 screen registry, same way as external plug-ins. */
@Module
@InstallIn(SingletonComponent::class)
internal interface FeedV2ScreensModule {

    @Binds
    @IntoMap
    @ClassKey(FeedRoute.MarketsTokenDetails::class)
    fun bindMarketsTokenDetailsScreenFactory(impl: FeedMarketsTokenDetailsScreenFactory): FeedScreenFactory

    @Binds
    @IntoMap
    @ClassKey(FeedRoute.ForYou::class)
    fun bindForYouScreenFactory(impl: FeedForYouScreenFactory): FeedScreenFactory
}