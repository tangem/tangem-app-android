package com.tangem.features.tangempay.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.tangempay.deeplink.DefaultTangemPayMainDeepLinkHandler
import com.tangem.features.tangempay.deeplink.DefaultTangemPayOnboardDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayAccountDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayMainDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayOnboardDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDeeplinkModule {

    @Binds
    @Singleton
    fun bindDeepLinkHandlerFactory(
        impl: DefaultTangemPayOnboardDeepLinkHandler.Factory,
    ): TangemPayOnboardDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindTangemPayMainDeepLinkHandlerFactory(
        impl: DefaultTangemPayMainDeepLinkHandler.Factory,
    ): TangemPayMainDeepLinkHandler.Factory

    @Binds
    @IntoMap
    @DeepLinkKey(TangemPayAccountDeepLinkHandler.DEEPLINK_KEY)
    fun bindTangemPayAccountDeepLinkHandler(impl: TangemPayAccountDeepLinkHandler): DeepLinkHandler

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultTangemPayOnboardDeepLinkHandler.TANGEM_DEEPLINK_KEY)
        fun provideTangemPayOnboardDeepLinkHandler(factory: TangemPayOnboardDeepLinkHandler.Factory): DeepLinkHandler =
            tangemPayOnboardDeepLinkHandler(factory)

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultTangemPayOnboardDeepLinkHandler.HTTPS_DEEPLINK_KEY)
        fun provideTangemPayOnboardHttpsDeepLinkHandler(
            factory: TangemPayOnboardDeepLinkHandler.Factory,
        ): DeepLinkHandler = tangemPayOnboardDeepLinkHandler(factory)

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultTangemPayMainDeepLinkHandler.TANGEM_DEEPLINK_KEY)
        fun provideTangemPayMainDeepLinkHandler(factory: TangemPayMainDeepLinkHandler.Factory): DeepLinkHandler =
            tangemPayMainDeepLinkHandler(factory)

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultTangemPayMainDeepLinkHandler.HTTPS_DEEPLINK_KEY)
        fun provideTangemPayMainHttpsDeepLinkHandler(factory: TangemPayMainDeepLinkHandler.Factory): DeepLinkHandler =
            tangemPayMainDeepLinkHandler(factory)

        private fun tangemPayOnboardDeepLinkHandler(factory: TangemPayOnboardDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(uri = request.uri) }

        private fun tangemPayMainDeepLinkHandler(factory: TangemPayMainDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(scope = request.scope, payload = request.queryParams) }
    }
}