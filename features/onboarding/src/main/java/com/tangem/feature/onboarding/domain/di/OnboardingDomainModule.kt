package com.tangem.feature.onboarding.domain.di

import com.tangem.feature.onboarding.domain.DummySeedPhraseInteractor
import com.tangem.feature.onboarding.domain.SeedPhraseInteractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class OnboardingDomainModule {

    @Provides
    @Singleton
    fun provideSeedPhraseInteractor(
    ): SeedPhraseInteractor {
        return DummySeedPhraseInteractor()
    }
}
