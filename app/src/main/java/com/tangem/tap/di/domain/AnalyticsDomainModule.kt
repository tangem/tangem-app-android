package com.tangem.tap.di.domain

import com.tangem.domain.analytics.CheckIsWalletToppedUpUseCase
import com.tangem.domain.analytics.repository.AnalyticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
internal object AnalyticsDomainModule {

    @Provides
    fun provideCheckIsWalletToppedUpUseCase(analyticsRepository: AnalyticsRepository): CheckIsWalletToppedUpUseCase {
        return CheckIsWalletToppedUpUseCase(analyticsRepository)
    }
}