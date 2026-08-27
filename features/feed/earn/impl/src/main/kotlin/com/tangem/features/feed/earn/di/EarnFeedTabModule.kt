package com.tangem.features.feed.earn.di

import com.tangem.features.feed.earn.components.EarnFeedTabContributor
import com.tangem.features.feed.nav.FeedTabContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnFeedTabModule {

    @Binds
    @IntoSet
    fun bindEarnFeedTabContributor(impl: EarnFeedTabContributor): FeedTabContributor
}