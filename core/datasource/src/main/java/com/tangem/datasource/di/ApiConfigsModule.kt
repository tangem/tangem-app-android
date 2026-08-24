package com.tangem.datasource.di

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.header.TangemApiKeyHeaderProvider
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.utils.TangemApiKeyHeader
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
internal object ApiConfigsModule {

    @Provides
    @IntoMap
    @StringKey(TangemTech.KEY)
    fun provideTangemTechConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemTech(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    fun provideTangemApiKeyHeaderProvider(authProvider: AuthProvider): TangemApiKeyHeaderProvider {
        return TangemApiKeyHeaderProvider { environment -> TangemApiKeyHeader(authProvider, environment) }
    }

    @Provides
    @IntoMap
    @StringKey(YieldSupply.KEY)
    fun provideYieldSupplyConfig(
        environmentConfig: EnvironmentConfig,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return YieldSupply(
            environmentConfig = environmentConfig,
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketWeb.KEY)
    fun providePolymarketWebConfig(): ApiConfig {
        return PolymarketWeb()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketRelayer.KEY)
    fun providePolymarketRelayerConfig(): ApiConfig {
        return PolymarketRelayer()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketClob.KEY)
    fun providePolymarketClobConfig(): ApiConfig {
        return PolymarketClob()
    }
}