package com.tangem.data.mcp.di

import com.tangem.core.remote.ApiSerialization
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
                apiConfigId = TangemGatewayApiConfig.TANGEM_TECH_ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = true,
                shouldSaveLogs = true,
                isLoggable = true,
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }
}