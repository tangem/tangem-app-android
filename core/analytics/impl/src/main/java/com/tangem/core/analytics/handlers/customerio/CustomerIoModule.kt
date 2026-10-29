package com.tangem.core.analytics.handlers.customerio

import android.app.Application
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.config.AnalyticsEnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CustomerIoModule {

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideCustomerIoHandler(application: Application, config: AnalyticsEnvironmentConfig): Set<AnalyticsHandler> {
        val cdpApiKey = config.customerIoCdpApiKey
        if (cdpApiKey.isNullOrBlank()) return emptySet()

        return setOf(CustomerIoAnalyticsHandler(CustomerIoClient(application = application, cdpApiKey = cdpApiKey)))
    }
}