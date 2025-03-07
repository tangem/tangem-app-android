package com.tangem.features.onboarding.v2.entry.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.DefaultOnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.model.OnboardingEntryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
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

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {
    @Binds
    @IntoMap
    @ClassKey(OnboardingEntryModel::class)
    fun provideModel(model: OnboardingEntryModel): Model
}