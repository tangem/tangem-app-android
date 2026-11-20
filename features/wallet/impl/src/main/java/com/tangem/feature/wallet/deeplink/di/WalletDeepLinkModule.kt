package com.tangem.feature.wallet.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.wallet.deeplink.DefaultWalletDeepLinkActionTrigger
import com.tangem.feature.wallet.deeplink.PromoDeeplinkHandler
import com.tangem.feature.wallet.deeplink.WalletDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(WalletDeepLinkHandler.DEEPLINK_KEY)
    fun bindWalletDeepLinkHandler(impl: WalletDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(PromoDeeplinkHandler.DEEPLINK_KEY)
    fun bindPromoDeepLinkHandler(impl: PromoDeeplinkHandler): DeepLinkHandler

    @Binds
    @Singleton
    fun bindWalletDeepLinkActionTrigger(impl: DefaultWalletDeepLinkActionTrigger): WalletDeepLinkActionTrigger

    @Binds
    @Singleton
    fun bindWalletDeepLinkActionListener(impl: DefaultWalletDeepLinkActionTrigger): WalletDeepLinkActionListener
}