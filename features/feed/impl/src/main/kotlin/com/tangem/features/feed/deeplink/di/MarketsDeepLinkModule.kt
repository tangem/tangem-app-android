package com.tangem.features.feed.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.feed.deeplink.DefaultMarketsTokenDetailDeepLinkHandler
import com.tangem.features.feed.deeplink.MarketsDeepLinkHandler
import com.tangem.features.feed.deeplink.MarketsTokenExchangesDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.MarketsTokenDetailDeepLinkHandler
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
    @IntoMap
    @DeepLinkKey(MarketsDeepLinkHandler.DEEPLINK_KEY)
    fun bindMarketsDeepLinkHandler(impl: MarketsDeepLinkHandler): DeepLinkHandler

    @Binds
    @Singleton
    fun bindMarketsTokenDetailDeepLinkHandlerFactory(
        impl: DefaultMarketsTokenDetailDeepLinkHandler.Factory,
    ): MarketsTokenDetailDeepLinkHandler.Factory

    @Binds
    @IntoMap
    @DeepLinkKey(MarketsTokenExchangesDeepLinkHandler.DEEPLINK_KEY)
    fun bindMarketsTokenExchangesDeepLinkHandler(impl: MarketsTokenExchangesDeepLinkHandler): DeepLinkHandler

    companion object {

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
    }
}