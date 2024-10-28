package com.tangem.core.analytics.di

import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.domain.analytics.repository.AnalyticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {

    @Singleton
    @Provides
    fun provideAnalyticsHandler(): AnalyticsEventHandler {
        return Analytics // todo replace after refactoring calling Analytics in whole project
    }

    @Singleton
    @Provides
    fun provideParamsInterceptorHolder(): ParamsInterceptorHolder {
        return Analytics // todo replace after refactoring calling Analytics in whole project
    }

    @Provides
    fun provideOneTimeEventFilter(analyticsRepository: AnalyticsRepository): OneTimeEventFilter {
        return OneTimeEventFilter(analyticsRepository)
    }
}