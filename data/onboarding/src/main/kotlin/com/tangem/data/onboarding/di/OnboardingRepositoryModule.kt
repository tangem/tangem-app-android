package com.tangem.data.onboarding.di

import com.tangem.data.onboarding.DefaultOnboardingRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.onboarding.repository.OnboardingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingRepositoryModule {

    @Provides
    @Singleton
    fun provideOnboardingRepository(appPreferencesStore: AppPreferencesStore): OnboardingRepository {
        return DefaultOnboardingRepository(appPreferencesStore = appPreferencesStore)
    }
}