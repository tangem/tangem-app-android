package com.tangem.features.send.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.send.deeplink.SellRedirectDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface SendDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(SellRedirectDeepLinkHandler.DEEPLINK_KEY)
    fun bindSellRedirectDeepLinkHandler(impl: SellRedirectDeepLinkHandler): DeepLinkHandler
}