package com.tangem.tap.di.domain

import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.repository.OnboardingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingDomainModule {

    @Provides
    @Singleton
    fun provideWasTwinsOnboardingShownUseCase(
        onboardingRepository: OnboardingRepository,
    ): WasTwinsOnboardingShownUseCase {
        return WasTwinsOnboardingShownUseCase(onboardingRepository)
    }

    @Provides
    @Singleton
    fun provideSaveTwinsOnboardingShownUseCase(
        onboardingRepository: OnboardingRepository,
    ): SaveTwinsOnboardingShownUseCase {
        return SaveTwinsOnboardingShownUseCase(onboardingRepository)
    }
}