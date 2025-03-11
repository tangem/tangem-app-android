package com.tangem.features.onboarding.v2.di

import com.tangem.features.onboarding.v2.DefaultOnboardingV2FeatureToggles
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
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
    fun provideFeatureToggles(featureToggles: DefaultOnboardingV2FeatureToggles): OnboardingV2FeatureToggles
}