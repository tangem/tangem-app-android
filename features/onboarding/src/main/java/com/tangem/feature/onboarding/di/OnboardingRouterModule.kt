package com.tangem.feature.onboarding.di

import com.tangem.feature.onboarding.navigation.DefaultOnboardingRouter
import com.tangem.feature.onboarding.navigation.OnboardingRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object OnboardingRouterModule {

    @Provides
    @ActivityScoped
    fun provideOnboardingRouter(): OnboardingRouter {
        return DefaultOnboardingRouter()
    }
}