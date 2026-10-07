package com.tangem.feature.wallet.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.wallet.deeplink.DefaultPromoDeeplinkHandler
import com.tangem.feature.wallet.deeplink.DefaultWalletDeepLinkActionTrigger
import com.tangem.feature.wallet.deeplink.DefaultWalletDeepLinkHandler
import com.tangem.features.wallet.deeplink.PromoDeeplinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
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

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultWalletDeepLinkHandler.DEEPLINK_KEY)
        fun provideWalletDeepLinkHandler(factory: WalletDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { factory.create() }

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultPromoDeeplinkHandler.DEEPLINK_KEY)
        fun providePromoDeepLinkHandler(factory: PromoDeeplinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
            }
    }
}