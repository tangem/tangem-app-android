package com.tangem.features.mcp.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.mcp.impl.deeplink.McpIntentDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface McpDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(McpIntentDeepLinkHandler.DEEPLINK_KEY)
    fun bindIntentDeepLinkHandler(impl: McpIntentDeepLinkHandler): DeepLinkHandler
}