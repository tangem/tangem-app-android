package com.tangem.tap.di.analytics

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.tap.common.analytics.DefaultTrackingContextProxy
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAppInstanceIdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}