package com.tangem.features.feed.deeplink.di

import com.tangem.features.feed.deeplink.DefaultMarketsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultMarketsTokenDetailDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsTokenDetailDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MarketsDeepLinkModule {

    @Binds
    @Singleton
    fun bindMarketsDeepLinkHandlerFactory(impl: DefaultMarketsDeepLinkHandler.Factory): MarketsDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindMarketsTokenDetailDeepLinkHandlerFactory(
        impl: DefaultMarketsTokenDetailDeepLinkHandler.Factory,
    ): MarketsTokenDetailDeepLinkHandler.Factory
}