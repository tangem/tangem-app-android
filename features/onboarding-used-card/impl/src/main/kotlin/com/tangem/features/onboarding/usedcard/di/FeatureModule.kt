package com.tangem.features.onboarding.usedcard.di

import com.tangem.features.onboarding.usedcard.DefaultUsedCardOnboardingFeatureToggles
import com.tangem.features.onboarding.usedcard.UsedCardOnboardingFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureModule {

    @Singleton
    @Binds
    fun provideFeatureToggles(
        featureToggles: DefaultUsedCardOnboardingFeatureToggles,
    ): UsedCardOnboardingFeatureToggles
}