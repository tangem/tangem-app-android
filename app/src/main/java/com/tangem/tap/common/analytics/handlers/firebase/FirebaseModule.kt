package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.domain.common.LogConfig
import com.tangem.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FirebaseModule {

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
}