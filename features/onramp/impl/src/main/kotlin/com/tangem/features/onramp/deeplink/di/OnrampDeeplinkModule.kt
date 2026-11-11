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
    @IntoMap
    @DeepLinkKey(BuyDeepLinkHandler.DEEPLINK_KEY)
    fun bindBuyDeepLinkHandler(impl: DefaultBuyDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(SellDeepLinkHandler.DEEPLINK_KEY)
    fun bindSellDeepLinkHandler(impl: SellDeepLinkHandler): DeepLinkHandler

    @Binds
    @Singleton
    fun bindConciergeWaitlistTrigger(impl: DefaultConciergeWaitlistTrigger): ConciergeWaitlistTrigger

    @Binds
    @Singleton
    fun bindConciergeWaitlistListener(impl: DefaultConciergeWaitlistTrigger): ConciergeWaitlistListener

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
    }
}