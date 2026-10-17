package com.tangem.features.onramp.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.onramp.deeplink.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampDeeplinkModule {

    @Binds
    @Singleton
    fun bindOnrampDeepLinkHandlerFactory(impl: DefaultOnrampDeepLinkHandler.Factory): OnrampDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindBuyDeepLinkHandler(impl: DefaultBuyDeepLinkHandler.Factory): BuyDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindSellDeepLinkHandler(impl: DefaultSellDeepLinkHandler.Factory): SellDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultOnrampDeepLinkHandler.DEEPLINK_KEY)
        fun provideOnrampDeepLinkHandler(factory: OnrampDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
            }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultBuyDeepLinkHandler.DEEPLINK_KEY)
        fun provideBuyDeepLinkHandler(factory: BuyDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { factory.create() }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultSellDeepLinkHandler.DEEPLINK_KEY)
        fun provideSellDeepLinkHandler(factory: SellDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { factory.create() }

        /** Claimed on purpose so the provider's return link counts as handled; there is nothing to open. */
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultBuyDeepLinkHandler.REDIRECT_DEEPLINK_KEY)
        fun provideBuyRedirectDeepLinkHandler(): DeepLinkHandler = DeepLinkHandler {}
    }
}