package com.tangem.tap.di.analytics

import android.app.Application
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.common.LogConfig
import com.tangem.tap.common.analytics.AnalyticsEventsLogger
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeClient
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerClient
import com.tangem.tap.common.analytics.handlers.appsflyer.AppsFlyerLogClient
import com.tangem.tap.common.analytics.handlers.customerio.CustomerIoAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.customerio.CustomerIoClient
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
    @IntoSet
    fun provideAmplitudeHandler(application: Application, environmentConfig: EnvironmentConfig): AnalyticsHandler {
        val client = if (BuildConfig.TESTER_MENU_ENABLED) {
            AmplitudeClient(
                application = application,
                key = requireNotNull(environmentConfig.amplitudeApiKeyDev) {
                    "Amplitude api key not found in ${BuildConfig.BUILD_TYPE}"
                },
                logger = AnalyticsEventsLogger(name = AmplitudeAnalyticsHandler.ID),
            )
        } else {
            AmplitudeClient(application = application, key = environmentConfig.amplitudeApiKey, logger = null)
        }

        return AmplitudeAnalyticsHandler(client)
    }

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
        environmentConfig: EnvironmentConfig,
        appsFlyerClientFactory: AppsFlyerClient.Factory,
    ): AnalyticsHandler {
        val client = if (LogConfig.analyticsHandlers.isAppsflyerLogEnabled) {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer log enabled, mock client created")
            AppsFlyerLogClient()
        } else {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer log disabled, real client created")
            appsFlyerClientFactory.create(apiKey = environmentConfig.appsFlyerApiKey)
        }

        return AppsFlyerAnalyticsHandler(client)
    }

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideCustomerIoHandler(
        application: Application,
        environmentConfig: EnvironmentConfig,
    ): Set<AnalyticsHandler> {
        val cdpApiKey = environmentConfig.customerIoCdpApiKey
        if (cdpApiKey.isNullOrBlank()) return emptySet()

        return setOf(CustomerIoAnalyticsHandler(CustomerIoClient(application = application, cdpApiKey = cdpApiKey)))
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