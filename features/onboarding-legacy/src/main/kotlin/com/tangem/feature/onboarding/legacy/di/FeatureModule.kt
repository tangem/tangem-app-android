package com.tangem.feature.onboarding.legacy.di

import com.tangem.feature.onboarding.legacy.featuretoggle.DefaultOnboardingLegacyFeatureToggles
import com.tangem.feature.onboarding.legacy.featuretoggle.OnboardingLegacyFeatureToggles
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
    fun provideFeatureToggles(featureToggles: DefaultOnboardingLegacyFeatureToggles): OnboardingLegacyFeatureToggles
}
