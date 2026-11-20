package com.tangem.feature.swap.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.swap.deeplink.SwapDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(SwapDeepLinkHandler.DEEPLINK_KEY)
    fun bindSwapDeepLinkHandler(impl: SwapDeepLinkHandler): DeepLinkHandler
}