package com.tangem.features.feed.deeplink.di

import com.tangem.features.feed.deeplink.DefaultEarnDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultNewsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultNewsDetailsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultYieldDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.EarnDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.YieldDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedDeepLinkModule {

    @Binds
    @Singleton
    fun bindNewsDetailsDeepLinkHandlerFactory(
        impl: DefaultNewsDetailsDeepLinkHandler.Factory,
    ): NewsDetailsDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindNewsDeepLinkHandlerFactory(impl: DefaultNewsDeepLinkHandler.Factory): NewsDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindEarnDeepLinkHandlerFactory(impl: DefaultEarnDeepLinkHandler.Factory): EarnDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindYieldDeepLinkHandlerFactory(impl: DefaultYieldDeepLinkHandler.Factory): YieldDeepLinkHandler.Factory
}