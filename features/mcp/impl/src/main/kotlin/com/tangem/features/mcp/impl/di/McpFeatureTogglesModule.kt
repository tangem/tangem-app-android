package com.tangem.features.mcp.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.mcp.api.McpFeatureToggles
import com.tangem.features.mcp.impl.DefaultMcpFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object McpFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideMcpFeatureToggles(featureTogglesManager: FeatureTogglesManager): McpFeatureToggles {
        return DefaultMcpFeatureToggles(featureTogglesManager)
    }
}