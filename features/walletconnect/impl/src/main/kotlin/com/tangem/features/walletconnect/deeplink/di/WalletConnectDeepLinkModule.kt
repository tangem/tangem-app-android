package com.tangem.features.walletconnect.deeplink.di

import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import com.tangem.features.walletconnect.deeplink.DefaultWalletConnectDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletConnectDeepLinkModule {

    @Binds
    @Singleton
    fun bindWalletConnectDeepLinkHandlerFactory(
        impl: DefaultWalletConnectDeepLinkHandler.Factory,
    ): WalletConnectDeepLinkHandler.Factory
}