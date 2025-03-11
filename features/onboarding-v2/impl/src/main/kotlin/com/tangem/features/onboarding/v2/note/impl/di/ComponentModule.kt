package com.tangem.features.onboarding.v2.note.impl.di

import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.DefaultOnboardingNoteComponent
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
    fun bindComponent(factory: DefaultOnboardingNoteComponent.Factory): OnboardingNoteComponent.Factory
}
