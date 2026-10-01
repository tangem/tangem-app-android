package com.tangem.feature.swap.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.swap.deeplink.DefaultSwapDeepLinkHandler
import com.tangem.features.swap.deeplink.SwapDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDeepLinkModule {

    @Binds
    @Singleton
    fun bindSwapDeepLinkHandlerFactory(impl: DefaultSwapDeepLinkHandler.Factory): SwapDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultSwapDeepLinkHandler.DEEPLINK_KEY)
        fun provideSwapDeepLinkHandler(factory: SwapDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
            }
    }
}