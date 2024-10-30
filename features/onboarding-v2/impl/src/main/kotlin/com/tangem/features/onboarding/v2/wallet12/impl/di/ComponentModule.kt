package com.tangem.features.onboarding.v2.wallet12.impl.di

import com.tangem.features.onboarding.v2.wallet12.api.OnboardingWallet12Component
import com.tangem.features.onboarding.v2.wallet12.impl.DefaultOnboardingWallet12Component
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
    fun bindOnboardingWallet12Component(
        factory: DefaultOnboardingWallet12Component.Factory,
    ): OnboardingWallet12Component.Factory
}
