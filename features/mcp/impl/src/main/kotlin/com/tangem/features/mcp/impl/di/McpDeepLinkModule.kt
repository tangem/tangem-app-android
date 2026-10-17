package com.tangem.features.mcp.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.mcp.api.McpFeatureToggles
import com.tangem.features.mcp.api.deeplink.McpIntentDeepLinkHandler
import com.tangem.features.mcp.impl.deeplink.DefaultMcpIntentDeepLinkHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal object McpDeepLinkModule {

    @Provides
    @IntoMap
    @DeepLinkKey(DefaultMcpIntentDeepLinkHandler.DEEPLINK_KEY)
    fun provideIntentDeepLinkHandler(
        factory: McpIntentDeepLinkHandler.Factory,
        mcpFeatureToggles: McpFeatureToggles,
    ): DeepLinkHandler = DeepLinkHandler(isEnabled = { mcpFeatureToggles.isMcpEnabled }) { request ->
        factory.create(request.queryParams)
    }
}