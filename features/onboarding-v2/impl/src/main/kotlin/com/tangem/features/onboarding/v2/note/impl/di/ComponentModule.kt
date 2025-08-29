package com.tangem.features.onboarding.v2.note.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.DefaultOnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.child.create.model.OnboardingNoteCreateWalletModel
import com.tangem.features.onboarding.v2.note.impl.model.OnboardingNoteModel
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
    fun bindComponent(factory: DefaultOnboardingNoteComponent.Factory): OnboardingNoteComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnboardingNoteModel::class)
    fun provideNoteModel(model: OnboardingNoteModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingNoteCreateWalletModel::class)
    fun provideNoteCreateWalletModel(model: OnboardingNoteCreateWalletModel): Model
}