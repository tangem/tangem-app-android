package com.tangem.core.analytics.handlers.appsflyer

import android.app.Application
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.config.AnalyticsEnvironmentConfig
import com.tangem.core.analytics.impl.BuildConfig
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
        config: AnalyticsEnvironmentConfig,
        appsFlyerClientFactory: AppsFlyerClient.Factory,
    ): AnalyticsHandler {
        val client = if (BuildConfig.LOG_ENABLED) {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer log enabled, mock client created")
            AppsFlyerLogClient()
        } else {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer log disabled, real client created")
            appsFlyerClientFactory.create(apiKey = config.appsFlyerApiKey)
        }

        return AppsFlyerAnalyticsHandler(client)
    }

    @Provides
    @IntoSet
    fun provideNewIntentObserver(observer: AppsFlyerNewIntentObserver): Application.ActivityLifecycleCallbacks {
        return observer
    }
}