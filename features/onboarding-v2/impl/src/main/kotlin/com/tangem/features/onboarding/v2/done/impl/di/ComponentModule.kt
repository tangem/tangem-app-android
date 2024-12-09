package com.tangem.features.onboarding.v2.done.impl.di

import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.done.impl.DefaultOnboardingDoneComponent
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
    fun componentFactory(factory: DefaultOnboardingDoneComponent.Factory): OnboardingDoneComponent.Factory
}