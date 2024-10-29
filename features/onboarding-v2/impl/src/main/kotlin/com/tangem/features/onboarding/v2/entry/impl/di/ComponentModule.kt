package com.tangem.features.onboarding.v2.entry.impl.di

import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.DefaultOnboardingEntryComponent
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
    fun bindOnboardingEntryComponent(
        factory: DefaultOnboardingEntryComponent.Factory,
    ): OnboardingEntryComponent.Factory
}
