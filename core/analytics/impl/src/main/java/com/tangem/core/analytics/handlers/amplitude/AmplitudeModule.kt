package com.tangem.core.analytics.handlers.amplitude

import android.app.Application
import com.tangem.core.analytics.AnalyticsEventsLogger
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.config.AnalyticsEnvironmentConfig
import com.tangem.core.analytics.impl.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AmplitudeModule {

    @Provides
    @Singleton
    @IntoSet
    fun provideAmplitudeHandler(application: Application, config: AnalyticsEnvironmentConfig): AnalyticsHandler {
        val client = if (BuildConfig.TESTER_MENU_ENABLED) {
            AmplitudeClient(
                application = application,
                key = requireNotNull(config.amplitudeApiKeyDev) {
                    "Amplitude api key not found in ${BuildConfig.BUILD_TYPE}"
                },
                logger = AnalyticsEventsLogger(name = AmplitudeAnalyticsHandler.ID),
            )
        } else {
            AmplitudeClient(application = application, key = config.amplitudeApiKey, logger = null)
        }

        return AmplitudeAnalyticsHandler(client)
    }
}