package com.tangem.features.send.v2.deeplink.di

import com.tangem.features.send.v2.api.deeplink.SellDeepLinkHandler
import com.tangem.features.send.v2.deeplink.DefaultSellDeepLinkHandler
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
    fun bindFactory(impl: DefaultSellDeepLinkHandler.Factory): SellDeepLinkHandler.Factory
}