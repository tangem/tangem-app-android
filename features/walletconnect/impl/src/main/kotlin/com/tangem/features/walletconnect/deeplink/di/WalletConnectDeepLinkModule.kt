package com.tangem.features.walletconnect.deeplink.di

import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkKey
import com.tangem.features.walletconnect.deeplink.WalletConnectDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletConnectDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(WalletConnectDeepLinkHandler.WC_DEEPLINK_KEY)
    fun bindWalletConnectDeepLinkHandler(impl: WalletConnectDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(WalletConnectDeepLinkHandler.TANGEM_DEEPLINK_KEY)
    fun bindTangemWalletConnectDeepLinkHandler(impl: WalletConnectDeepLinkHandler): DeepLinkHandler
}