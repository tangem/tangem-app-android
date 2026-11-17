package com.tangem.features.feed.deeplink.di

import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkKey
import com.tangem.features.feed.deeplink.DiscoverDeepLinkHandler
import com.tangem.features.feed.deeplink.NewsDeepLinkHandler
import com.tangem.features.feed.deeplink.NewsWebDeepLinkHandler
import com.tangem.features.feed.deeplink.YieldDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedDeepLinkModule {

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
    @DeepLinkKey(DiscoverDeepLinkHandler.DEEPLINK_KEY)
    fun bindDiscoverDeepLinkHandler(impl: DiscoverDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(YieldDeepLinkHandler.DEEPLINK_KEY)
    fun bindYieldDeepLinkHandler(impl: YieldDeepLinkHandler): DeepLinkHandler
}