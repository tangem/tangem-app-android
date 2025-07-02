package com.tangem.feature.tokendetails.deeplink.di

import com.tangem.feature.tokendetails.deeplink.DefaultTokenDetailsDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenDetailsDeepLinkModule {

    @Binds
    @Singleton
    fun bindWalletDeepLinkHandlerFactory(
        impl: DefaultTokenDetailsDeepLinkHandler.Factory,
    ): TokenDetailsDeepLinkHandler.Factory
}