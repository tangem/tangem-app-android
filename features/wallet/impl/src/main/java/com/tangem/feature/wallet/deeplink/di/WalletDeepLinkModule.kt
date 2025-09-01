package com.tangem.feature.wallet.deeplink.di

import com.tangem.feature.wallet.deeplink.DefaultPromoDeeplinkHandler
import com.tangem.feature.wallet.deeplink.DefaultWalletDeepLinkActionTrigger
import com.tangem.feature.wallet.deeplink.DefaultWalletDeepLinkHandler
import com.tangem.features.wallet.deeplink.PromoDeeplinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletDeepLinkModule {

    @Binds
    @Singleton
    fun bindWalletDeepLinkHandlerFactory(impl: DefaultWalletDeepLinkHandler.Factory): WalletDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindPromoDeepLinkHandlerFactory(impl: DefaultPromoDeeplinkHandler.Factory): PromoDeeplinkHandler.Factory

    @Binds
    @Singleton
    fun bindWalletDeepLinkActionTrigger(impl: DefaultWalletDeepLinkActionTrigger): WalletDeepLinkActionTrigger

    @Binds
    @Singleton
    fun bindWalletDeepLinkActionListener(impl: DefaultWalletDeepLinkActionTrigger): WalletDeepLinkActionListener
}