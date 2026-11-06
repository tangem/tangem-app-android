package com.tangem.tap.di.data

import com.tangem.core.analytics.config.AnalyticsEnvironmentConfig
import com.tangem.core.analytics.handlers.opentelemetry.OtelAccessTokenProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.lib.auth.session.SessionTokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsConfigModule {

    @Provides
    @Singleton
    fun provideAnalyticsEnvironmentConfig(environmentConfig: EnvironmentConfig): AnalyticsEnvironmentConfig {
        return AnalyticsEnvironmentConfig(
            amplitudeApiKey = environmentConfig.amplitudeApiKey,
            amplitudeApiKeyDev = environmentConfig.amplitudeApiKeyDev,
            appsFlyerApiKey = environmentConfig.appsFlyerApiKey,
            customerIoCdpApiKey = environmentConfig.customerIoCdpApiKey,
        )
    }

    @Provides
    @Singleton
    fun provideOtelAccessTokenProvider(sessionTokenRefresher: SessionTokenRefresher): OtelAccessTokenProvider {
        return OtelAccessTokenProvider { sessionTokenRefresher.ensureSession().getOrNull()?.accessToken }
    }
}