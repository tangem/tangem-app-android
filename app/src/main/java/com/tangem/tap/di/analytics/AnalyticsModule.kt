package com.tangem.tap.di.analytics

import com.tangem.domain.analytics.ChangeCardAnalyticsContextUseCase
import com.tangem.tap.common.analytics.DefaultChangeCardAnalyticsContextUseCase
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
    fun provideChangeCardAnalyticsContextUseCase(): ChangeCardAnalyticsContextUseCase {
        return DefaultChangeCardAnalyticsContextUseCase()
    }
}