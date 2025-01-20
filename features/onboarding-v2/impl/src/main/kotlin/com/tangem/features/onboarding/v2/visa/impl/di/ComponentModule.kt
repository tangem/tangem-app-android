package com.tangem.features.onboarding.v2.visa.impl.di

import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
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
    fun bindComponentFactory(factory: DefaultOnboardingVisaComponent.Factory): OnboardingVisaComponent.Factory
}