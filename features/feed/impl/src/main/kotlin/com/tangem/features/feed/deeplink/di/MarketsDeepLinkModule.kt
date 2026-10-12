package com.tangem.features.feed.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.feed.deeplink.DefaultMarketsDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultMarketsTokenDetailDeepLinkHandler
import com.tangem.features.feed.deeplink.DefaultMarketsTokenExchangesDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsTokenDetailDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsTokenExchangesDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
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

    @Binds
    @Singleton
    fun bindMarketsTokenExchangesDeepLinkHandlerFactory(
        impl: DefaultMarketsTokenExchangesDeepLinkHandler.Factory,
    ): MarketsTokenExchangesDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultMarketsDeepLinkHandler.DEEPLINK_KEY)
        fun provideMarketsDeepLinkHandler(factory: MarketsDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(params = request.queryParams) }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultMarketsTokenDetailDeepLinkHandler.DEEPLINK_KEY)
        fun provideMarketsTokenDetailDeepLinkHandler(
            factory: MarketsTokenDetailDeepLinkHandler.Factory,
        ): DeepLinkHandler = DeepLinkHandler { request ->
            factory.create(coroutineScope = request.scope, params = request.queryParams)
        }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultMarketsTokenExchangesDeepLinkHandler.DEEPLINK_KEY)
        fun provideMarketsTokenExchangesDeepLinkHandler(
            factory: MarketsTokenExchangesDeepLinkHandler.Factory,
        ): DeepLinkHandler = DeepLinkHandler { request ->
            factory.create(coroutineScope = request.scope, params = request.queryParams)
        }
    }
}