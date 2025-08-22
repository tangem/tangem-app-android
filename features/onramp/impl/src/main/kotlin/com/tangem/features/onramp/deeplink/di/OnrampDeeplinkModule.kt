package com.tangem.features.onramp.deeplink.di

import com.tangem.features.onramp.deeplink.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

    @Binds
    @Singleton
    fun bindSwapDeepLinkHandler(impl: DefaultSwapDeepLinkHandler.Factory): SwapDeepLinkHandler.Factory
}