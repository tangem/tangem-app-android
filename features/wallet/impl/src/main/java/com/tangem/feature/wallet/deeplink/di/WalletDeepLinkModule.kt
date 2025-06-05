package com.tangem.feature.wallet.deeplink.di

import com.tangem.feature.wallet.deeplink.DefaultWalletDeepLinkHandler
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
}