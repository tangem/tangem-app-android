package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.tangem.core.analytics.AnalyticsEventsLogger
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.domain.common.LogConfig
import com.tangem.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OpenTelemetryModule {

    @Provides
    @Singleton
    @IntoSet
    fun provideOpenTelemetryHandler(metricsHolder: OpenTelemetryMetricsHolder): AnalyticsHandler {
        val logger = if (BuildConfig.TESTER_MENU_ENABLED && LogConfig.analyticsHandlers.isOpenTelemetryLogEnabled) {
            AnalyticsEventsLogger(name = OpenTelemetryAnalyticsHandler.ID)
        } else {
            null
        }

        return OpenTelemetryAnalyticsHandler(metricsHolder = metricsHolder, logger = logger)
    }
}