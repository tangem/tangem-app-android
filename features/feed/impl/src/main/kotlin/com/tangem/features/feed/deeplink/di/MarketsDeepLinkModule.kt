package com.tangem.features.feed.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.feed.deeplink.MarketsDeepLinkHandler
import com.tangem.features.feed.deeplink.MarketsTokenDetailDeepLinkHandler
import com.tangem.features.feed.deeplink.MarketsTokenExchangesDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface MarketsDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(MarketsDeepLinkHandler.DEEPLINK_KEY)
    fun bindMarketsDeepLinkHandler(impl: MarketsDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(MarketsTokenDetailDeepLinkHandler.DEEPLINK_KEY)
    fun bindMarketsTokenDetailDeepLinkHandler(impl: MarketsTokenDetailDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(MarketsTokenExchangesDeepLinkHandler.DEEPLINK_KEY)
    fun bindMarketsTokenExchangesDeepLinkHandler(impl: MarketsTokenExchangesDeepLinkHandler): DeepLinkHandler
}