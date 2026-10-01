package com.tangem.features.feed.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.feed.deeplink.DefaultDiscoverDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultEarnDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultNewsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultNewsDetailsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultYieldDeepLinkHandler
import com.tangem.features.feed.deeplink.NewsWebDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.DiscoverDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.EarnDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDeepLinkHandler
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
    fun bindNewsDeepLinkHandlerFactory(impl: DefaultNewsDeepLinkHandler.Factory): NewsDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindEarnDeepLinkHandlerFactory(impl: DefaultEarnDeepLinkHandler.Factory): EarnDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindYieldDeepLinkHandlerFactory(impl: DefaultYieldDeepLinkHandler.Factory): YieldDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindDiscoverDeepLinkHandlerFactory(
        impl: DefaultDiscoverDeepLinkHandler.Factory,
    ): DiscoverDeepLinkHandler.Factory

    @Binds
    @IntoMap
    @DeepLinkKey(NewsWebDeepLinkHandler.DEEPLINK_KEY)
    fun bindNewsWebDeepLinkHandler(impl: NewsWebDeepLinkHandler): DeepLinkHandler

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultNewsDeepLinkHandler.DEEPLINK_KEY)
        fun provideNewsDeepLinkHandler(factory: NewsDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(queryParams = request.queryParams) }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultEarnDeepLinkHandler.DEEPLINK_KEY)
        fun provideEarnDeepLinkHandler(factory: EarnDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(queryParams = request.queryParams) }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultYieldDeepLinkHandler.DEEPLINK_KEY)
        fun provideYieldDeepLinkHandler(factory: YieldDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
            }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultDiscoverDeepLinkHandler.DEEPLINK_KEY)
        fun provideDiscoverDeepLinkHandler(factory: DiscoverDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(queryParams = request.queryParams) }
    }
}