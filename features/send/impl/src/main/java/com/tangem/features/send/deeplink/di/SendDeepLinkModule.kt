package com.tangem.features.send.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.features.send.api.deeplink.SellRedirectDeepLinkHandler
import com.tangem.features.send.deeplink.DefaultSellRedirectDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SendDeepLinkModule {

    @Binds
    @Singleton
    fun bindFactory(impl: DefaultSellRedirectDeepLinkHandler.Factory): SellRedirectDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        /** Not openable from push: the link prefills the Send screen with a payload-supplied address. */
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultSellRedirectDeepLinkHandler.DEEPLINK_KEY)
        fun provideSellRedirectDeepLinkHandler(factory: SellRedirectDeepLinkHandler.Factory): DeepLinkHandler =
            object : DeepLinkHandler {
                override val isOpenableFromPush: Boolean = false

                override fun handle(request: DeepLinkRequest) {
                    factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
                }
            }
    }
}