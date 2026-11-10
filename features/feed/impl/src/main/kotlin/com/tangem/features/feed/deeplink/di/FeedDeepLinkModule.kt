package com.tangem.features.feed.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.feed.deeplink.DefaultNewsDetailsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultYieldDeepLinkHandler
import com.tangem.features.feed.deeplink.DiscoverDeepLinkHandler
import com.tangem.features.feed.deeplink.EarnDeepLinkHandler
import com.tangem.features.feed.deeplink.NewsDeepLinkHandler
import com.tangem.features.feed.deeplink.NewsWebDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.YieldDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
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
    fun bindYieldDeepLinkHandlerFactory(impl: DefaultYieldDeepLinkHandler.Factory): YieldDeepLinkHandler.Factory

    @Binds
    @IntoMap
    @DeepLinkKey(NewsWebDeepLinkHandler.DEEPLINK_KEY)
    fun bindNewsWebDeepLinkHandler(impl: NewsWebDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(NewsDeepLinkHandler.DEEPLINK_KEY)
    fun bindNewsDeepLinkHandler(impl: NewsDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(EarnDeepLinkHandler.DEEPLINK_KEY)
    fun bindEarnDeepLinkHandler(impl: EarnDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(DiscoverDeepLinkHandler.DEEPLINK_KEY)
    fun bindDiscoverDeepLinkHandler(impl: DiscoverDeepLinkHandler): DeepLinkHandler

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultYieldDeepLinkHandler.DEEPLINK_KEY)
        fun provideYieldDeepLinkHandler(factory: YieldDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
            }
    }
}