package com.tangem.features.feed.deeplink.di

import com.tangem.features.feed.deeplink.DefaultNewsDetailsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
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
}