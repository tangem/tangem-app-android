package com.tangem.features.tangempay.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.tangempay.deeplink.DefaultOnboardVisaDeepLinkHandler
import com.tangem.features.tangempay.deeplink.DefaultTangemPayAccountDeepLinkHandler
import com.tangem.features.tangempay.deeplink.DefaultTangemPayMainDeepLinkHandler
import com.tangem.features.tangempay.deeplink.OnboardVisaDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayAccountDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayMainDeepLinkHandler
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
    fun bindDeepLinkHandlerFactory(impl: DefaultOnboardVisaDeepLinkHandler.Factory): OnboardVisaDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindTangemPayMainDeepLinkHandlerFactory(
        impl: DefaultTangemPayMainDeepLinkHandler.Factory,
    ): TangemPayMainDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindTangemPayAccountDeepLinkHandlerFactory(
        impl: DefaultTangemPayAccountDeepLinkHandler.Factory,
    ): TangemPayAccountDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultOnboardVisaDeepLinkHandler.TANGEM_DEEPLINK_KEY)
        fun provideOnboardVisaDeepLinkHandler(factory: OnboardVisaDeepLinkHandler.Factory): DeepLinkHandler =
            onboardVisaDeepLinkHandler(factory)

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultOnboardVisaDeepLinkHandler.HTTPS_DEEPLINK_KEY)
        fun provideOnboardVisaHttpsDeepLinkHandler(factory: OnboardVisaDeepLinkHandler.Factory): DeepLinkHandler =
            onboardVisaDeepLinkHandler(factory)

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

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultTangemPayAccountDeepLinkHandler.DEEPLINK_KEY)
        fun provideTangemPayAccountDeepLinkHandler(factory: TangemPayAccountDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(scope = request.scope, queryParams = request.queryParams) }

        private fun onboardVisaDeepLinkHandler(factory: OnboardVisaDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(uri = request.uri) }

        private fun tangemPayMainDeepLinkHandler(factory: TangemPayMainDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(scope = request.scope, payload = request.queryParams) }
    }
}