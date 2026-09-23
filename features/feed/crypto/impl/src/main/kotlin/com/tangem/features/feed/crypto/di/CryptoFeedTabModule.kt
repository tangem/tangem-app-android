package com.tangem.features.feed.crypto.di

import com.tangem.features.feed.crypto.CryptoFeedEmptySearchComponent
import com.tangem.features.feed.crypto.CryptoFeedSearchTabComponent
import com.tangem.features.feed.crypto.CryptoFeedTabComponent
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.categories.components.MarketCategorySelectorScreenFactory
import com.tangem.features.feed.crypto.components.CryptoFeedTabContributor
import com.tangem.features.feed.crypto.components.DefaultCryptoFeedEmptySearchComponent
import com.tangem.features.feed.crypto.components.DefaultCryptoFeedSearchTabComponent
import com.tangem.features.feed.crypto.components.DefaultCryptoFeedTabComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.nav.FeedTabContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface CryptoFeedTabModule {

    @Binds
    fun bindCryptoFeedTabComponentFactory(impl: DefaultCryptoFeedTabComponent.Factory): CryptoFeedTabComponent.Factory

    @Binds
    fun bindCryptoFeedSearchTabComponentFactory(
        impl: DefaultCryptoFeedSearchTabComponent.Factory,
    ): CryptoFeedSearchTabComponent.Factory

    @Binds
    fun bindCryptoFeedEmptySearchComponentFactory(
        impl: DefaultCryptoFeedEmptySearchComponent.Factory,
    ): CryptoFeedEmptySearchComponent.Factory

    @Binds
    @IntoSet
    fun bindCryptoFeedTabContributor(impl: CryptoFeedTabContributor): FeedTabContributor

    @Binds
    @IntoMap
    @ClassKey(MarketCategorySelectorRoute::class)
    fun bindMarketCategorySelectorScreenFactory(impl: MarketCategorySelectorScreenFactory): FeedScreenFactory
}