package com.tangem.tap.di.data

import com.tangem.core.analytics.config.AnalyticsEnvironmentConfig
import com.tangem.core.analytics.handlers.opentelemetry.OtelAccessTokenProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.lib.auth.session.SessionTokenRefresher
import dagger.Lazy
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

    // Lazy keeps the auth graph (AuthApi, Retrofit) out of the metrics holder creation at app start;
    // the token is first needed on the background drain
    @Provides
    @Singleton
    fun provideOtelAccessTokenProvider(sessionTokenRefresher: Lazy<SessionTokenRefresher>): OtelAccessTokenProvider {
        return OtelAccessTokenProvider { sessionTokenRefresher.get().ensureSession().getOrNull()?.accessToken }
    }
}