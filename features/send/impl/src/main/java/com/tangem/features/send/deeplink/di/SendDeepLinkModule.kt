package com.tangem.features.send.deeplink.di

import com.tangem.features.send.api.deeplink.SellRedirectDeepLinkHandler
import com.tangem.features.send.deeplink.DefaultSellRedirectDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SendDeepLinkModule {

    @Binds
    @Singleton
    fun bindFactory(impl: DefaultSellRedirectDeepLinkHandler.Factory): SellRedirectDeepLinkHandler.Factory
}