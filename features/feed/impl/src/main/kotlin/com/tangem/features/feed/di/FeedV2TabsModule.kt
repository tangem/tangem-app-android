package com.tangem.features.feed.di

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.components.v2.tabs.PlaceholderFeedTabContributor
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Placeholder tabs contributed by the feed itself. TODO: [TWI-1608] delete as real tab modules land. */
@Module
@InstallIn(SingletonComponent::class)
internal object FeedV2TabsModule {

    @Provides
    @IntoSet
    fun provideRealAssetsTab(): FeedTabContributor = PlaceholderFeedTabContributor(
        id = FeedTabId.RealAssets,
        title = stringReference("Real assets"),
    )

    @Provides
    @IntoSet
    fun provideEarnTab(): FeedTabContributor = PlaceholderFeedTabContributor(
        id = FeedTabId.Earn,
        title = stringReference("Earn"),
    )

    @Provides
    @IntoSet
    fun providePredictionsTab(): FeedTabContributor = PlaceholderFeedTabContributor(
        id = FeedTabId.Predictions,
        title = stringReference("Predictions"),
    )
}