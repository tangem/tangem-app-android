package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.common.LogConfig
import com.tangem.utils.logging.TangemLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppsFlyerModule {

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
}