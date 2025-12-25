package com.tangem.tap.di.analytics

import android.content.Context
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.analytics.AppsflyerConversionHandler
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.tap.common.analytics.DefaultTrackingContextProxy
import com.tangem.tap.common.analytics.appsflyer.DefaultAppsflyerConversionHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAppInstanceIdProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsContextProxy(abtestsManager: ABTestsManager): TrackingContextProxy =
        DefaultTrackingContextProxy(abtestsManager)

    @Provides
    @Singleton
    fun provideAppInstanceIdProvider(): AppInstanceIdProvider = FirebaseAppInstanceIdProvider()

    @Provides
    @Singleton
    fun provideAppsflyerConversionHandler(
        @ApplicationContext applicationContext: Context,
        appPreferencesStore: AppPreferencesStore,
        environmentConfigStorage: EnvironmentConfigStorage,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): AppsflyerConversionHandler {
        return DefaultAppsflyerConversionHandler(
            context = applicationContext,
            environmentConfigStorage = environmentConfigStorage,
            appPreferencesStore = appPreferencesStore,
            dispatchersProvider = coroutineDispatcherProvider,
        )
    }
}