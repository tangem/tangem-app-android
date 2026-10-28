package com.tangem.feature.usedesk.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles
import com.tangem.feature.usedesk.api.deeplink.SupportDeepLinkHandler
import com.tangem.feature.usedesk.deeplink.DefaultSupportDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SupportDeepLinkModule {

    @Binds
    @Singleton
    fun bindSupportDeepLinkHandlerFactory(impl: DefaultSupportDeepLinkHandler.Factory): SupportDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultSupportDeepLinkHandler.DEEPLINK_KEY)
        fun provideSupportDeepLinkHandler(
            factory: SupportDeepLinkHandler.Factory,
            useDeskFeatureToggles: UseDeskFeatureToggles,
        ): DeepLinkHandler = DeepLinkHandler(
            isEnabled = { useDeskFeatureToggles.isSupportChatDeepLinkEnabled },
            handle = { request -> factory.create(coroutineScope = request.scope) },
        )
    }
}