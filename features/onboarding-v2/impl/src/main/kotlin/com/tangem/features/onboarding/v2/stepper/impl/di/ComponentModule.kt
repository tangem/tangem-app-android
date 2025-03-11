package com.tangem.features.onboarding.v2.stepper.impl.di

import com.tangem.features.onboarding.v2.stepper.api.OnboardingStepperComponent
import com.tangem.features.onboarding.v2.stepper.impl.DefaultOnboardingStepperComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindOnboardingStepperComponent(
        factory: DefaultOnboardingStepperComponent.Factory,
    ): OnboardingStepperComponent.Factory
}