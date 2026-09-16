package com.tangem.features.feed.rwa.di

import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.rwa.components.RwaFeedTabContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface RwaFeedTabModule {

    @Binds
    @IntoSet
    fun bindRwaFeedTabContributor(impl: RwaFeedTabContributor): FeedTabContributor
}