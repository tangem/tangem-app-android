package com.tangem.features.feed.crypto.di

import com.tangem.features.feed.crypto.CryptoFeedTabComponent
import com.tangem.features.feed.crypto.components.CryptoFeedTabContributor
import com.tangem.features.feed.crypto.components.DefaultCryptoFeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface CryptoFeedTabModule {

    @Binds
    fun bindCryptoFeedTabComponentFactory(impl: DefaultCryptoFeedTabComponent.Factory): CryptoFeedTabComponent.Factory

    @Binds
    @IntoSet
    fun bindCryptoFeedTabContributor(impl: CryptoFeedTabContributor): FeedTabContributor
}