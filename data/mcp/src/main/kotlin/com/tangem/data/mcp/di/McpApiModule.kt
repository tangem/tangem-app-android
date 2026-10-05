package com.tangem.data.mcp.di

import com.tangem.core.remote.ApiSerialization
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
import com.tangem.data.mcp.api.McpApi
import com.tangem.lib.auth.api.config.Auth
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
                // The same config the session tokens are minted with: `TangemTech` defaults to PROD
                // on debug and internal builds while `Auth` defaults to DEV, so splitting them would
                // send dev tokens to the production gateway.
                apiConfigId = Auth.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = true,
                shouldSaveLogs = true,
                isLoggable = true,
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }
}