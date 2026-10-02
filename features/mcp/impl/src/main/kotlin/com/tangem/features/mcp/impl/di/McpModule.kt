package com.tangem.features.mcp.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.mcp.api.McpComponentFactory
import com.tangem.features.mcp.api.McpFeatureToggles
import com.tangem.features.mcp.api.deeplink.McpDeepLinkHandler
import com.tangem.features.mcp.impl.DefaultMcpComponentFactory
import com.tangem.features.mcp.impl.deeplink.DefaultMcpDeepLinkHandler
import com.tangem.features.mcp.impl.pair.model.McpPairModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureModule {

    @Binds
    @Singleton
    fun bindComponentFactory(impl: DefaultMcpComponentFactory): McpComponentFactory

    @Binds
    @Singleton
    fun bindDeepLinkHandlerFactory(impl: DefaultMcpDeepLinkHandler.Factory): McpDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultMcpDeepLinkHandler.DEEPLINK_KEY)
        fun provideMcpDeepLinkHandler(
            factory: McpDeepLinkHandler.Factory,
            mcpFeatureToggles: McpFeatureToggles,
        ): DeepLinkHandler = DeepLinkHandler(
            isEnabled = { mcpFeatureToggles.isMcpEnabled },
            handle = { request -> factory.create(queryParams = request.queryParams) },
        )
    }
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(McpPairModel::class)
    fun provideModel(model: McpPairModel): Model
}