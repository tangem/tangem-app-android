package com.tangem.feature.swap.deeplink.di

import com.tangem.feature.swap.deeplink.DefaultSwapDeepLinkHandler
import com.tangem.features.swap.deeplink.SwapDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDeepLinkModule {

    @Binds
    @Singleton
    fun bindSwapDeepLinkHandlerFactory(impl: DefaultSwapDeepLinkHandler.Factory): SwapDeepLinkHandler.Factory
}