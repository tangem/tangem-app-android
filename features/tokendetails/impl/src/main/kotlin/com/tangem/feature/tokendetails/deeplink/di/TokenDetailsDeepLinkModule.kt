package com.tangem.feature.tokendetails.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.tokendetails.deeplink.DefaultExpressDeepLinkTrigger
import com.tangem.feature.tokendetails.deeplink.DefaultTokenDetailsDeepLinkHandler
import com.tangem.feature.tokendetails.deeplink.TokenDetailsDeepLinkActionTrigger
import com.tangem.features.tokendetails.deeplink.ExpressDeepLinkListener
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenDetailsDeepLinkModule {

    @Binds
    @Singleton
    fun bindWalletDeepLinkHandlerFactory(
        impl: DefaultTokenDetailsDeepLinkHandler.Factory,
    ): TokenDetailsDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindTokenDetailsDeepLinkActionTrigger(impl: DefaultExpressDeepLinkTrigger): TokenDetailsDeepLinkActionTrigger

    @Binds
    @Singleton
    fun bindTokenDetailsDeepLinkActionListener(impl: DefaultExpressDeepLinkTrigger): ExpressDeepLinkListener

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultTokenDetailsDeepLinkHandler.DEEPLINK_KEY)
        fun provideTokenDetailsDeepLinkHandler(factory: TokenDetailsDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(
                    coroutineScope = request.scope,
                    queryParams = request.queryParams,
                    isFromOnNewIntent = request.isFromOnNewIntent,
                )
            }
    }
}