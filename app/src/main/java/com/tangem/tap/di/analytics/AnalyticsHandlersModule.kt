package com.tangem.tap.di.analytics

import com.tangem.core.analytics.AnalyticsEventsLogger
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.config.AnalyticsEnvironmentConfig
import com.tangem.domain.common.LogConfig
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerClient
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerLogClient
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseClient
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseLogClient
import com.tangem.tap.common.analytics.handlers.opentelemetry.OpenTelemetryAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.opentelemetry.OpenTelemetryMetricsHolder
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsHandlersModule {

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideFirebaseHandler(): Set<AnalyticsHandler> {
        val client = when {
            !BuildConfig.DEBUG -> FirebaseClient()
            LogConfig.analyticsHandlers.isFirebaseLogEnabled -> FirebaseLogClient()
            else -> return emptySet()
        }

        return setOf(FirebaseAnalyticsHandler(client))
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideAppsFlyerHandler(
        config: AnalyticsEnvironmentConfig,
        appsFlyerClientFactory: AppsFlyerClient.Factory,
    ): AnalyticsHandler {
        val client = if (LogConfig.analyticsHandlers.isAppsflyerLogEnabled) {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer log enabled, mock client created")
            AppsFlyerLogClient()
        } else {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer log disabled, real client created")
            appsFlyerClientFactory.create(apiKey = config.appsFlyerApiKey)
        }

        return AppsFlyerAnalyticsHandler(client)
    }

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