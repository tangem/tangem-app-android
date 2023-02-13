package com.tangem.core.analytics.di

import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AnalyticsModule {

    @Singleton
    @Provides
    fun provideAnalyticsHandler(): AnalyticsEventHandler {
        return Analytics // todo replace after refactoring calling Analytics in whole project
    }
}
