package com.tangem.features.feed.di

import com.tangem.features.feed.components.v2.DefaultFeedSearchBarController
import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.search.FeedSearchBarController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedV2NavModule {

    /**
     * Registry of feed screens, keyed by [com.tangem.features.feed.nav.FeedRoute] class. Features
     * contribute entries with `@Binds @IntoMap @ClassKey(FeedRoute.Foo::class)`; this declaration
     * only guards the empty-map case.
     */
    @Multibinds
    fun feedScreenFactories(): Map<Class<*>, FeedScreenFactory>

    /** Tab set of the feed home; features contribute with `@Binds/@Provides @IntoSet`. */
    @Multibinds
    fun feedTabContributors(): Set<FeedTabContributor>

    @Binds
    @Singleton
    fun bindFeedSearchBarController(impl: DefaultFeedSearchBarController): FeedSearchBarController
}