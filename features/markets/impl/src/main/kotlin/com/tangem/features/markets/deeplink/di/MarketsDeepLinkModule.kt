package com.tangem.features.markets.deeplink.di

import com.tangem.features.markets.deeplink.DefaultMarketsDeepLinkHandler
import com.tangem.features.markets.deeplink.DefaultMarketsTokenDetailDeepLinkHandler
import com.tangem.features.markets.deeplink.MarketsDeepLinkHandler
import com.tangem.features.markets.deeplink.MarketsTokenDetailDeepLinkHandler
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