package com.tangem.features.mcp.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.mcp.api.McpComponentFactory
import com.tangem.features.mcp.api.deeplink.McpDeepLinkHandler
import com.tangem.features.mcp.impl.DefaultMcpComponentFactory
import com.tangem.features.mcp.impl.deeplink.DefaultMcpDeepLinkHandler
import com.tangem.features.mcp.impl.pair.model.McpPairModel
import dagger.Binds
import dagger.Module
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
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(McpPairModel::class)
    fun provideModel(model: McpPairModel): Model
}