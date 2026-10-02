package com.tangem.data.mcp.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.data.mcp.api.McpApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object McpApiModule {

    @Provides
    @Singleton
    fun provideMcpApi(factory: RetrofitFactory): McpApi {
        return factory.build(
            RetrofitApiSpec(
                // Attaches to the gateway config by id, so this module needs nothing from the one
                // that owns the config implementation.
                apiConfigId = TangemGatewayApiConfig.TANGEM_TECH_ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = true,
                shouldSaveLogs = true,
                isLoggable = true,
            ),
        )
    }
}