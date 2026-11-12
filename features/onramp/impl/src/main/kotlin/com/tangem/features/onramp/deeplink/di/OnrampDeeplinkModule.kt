package com.tangem.features.onramp.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.onramp.deeplink.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampDeeplinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(OnrampDeepLinkHandler.DEEPLINK_KEY)
    fun bindOnrampDeepLinkHandler(impl: DefaultOnrampDeepLinkHandler): DeepLinkHandler

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
}