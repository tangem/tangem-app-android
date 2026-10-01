package com.tangem.features.walletconnect.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import com.tangem.features.walletconnect.deeplink.DefaultWalletConnectDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletConnectDeepLinkModule {

    @Binds
    @Singleton
    fun bindWalletConnectDeepLinkHandlerFactory(
        impl: DefaultWalletConnectDeepLinkHandler.Factory,
    ): WalletConnectDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultWalletConnectDeepLinkHandler.WC_DEEPLINK_KEY)
        fun provideWalletConnectDeepLinkHandler(factory: WalletConnectDeepLinkHandler.Factory): DeepLinkHandler =
            walletConnectDeepLinkHandler(factory)

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultWalletConnectDeepLinkHandler.TANGEM_DEEPLINK_KEY)
        fun provideTangemWalletConnectDeepLinkHandler(factory: WalletConnectDeepLinkHandler.Factory): DeepLinkHandler =
            walletConnectDeepLinkHandler(factory)

        /** Pairing with a payload-supplied session must never start from a push notification. */
        private fun walletConnectDeepLinkHandler(factory: WalletConnectDeepLinkHandler.Factory) =
            object : DeepLinkHandler {
                override val isOpenableFromPush: Boolean = false

                override fun handle(request: DeepLinkRequest) {
                    factory.create(uri = request.uri)
                }
            }
    }
}