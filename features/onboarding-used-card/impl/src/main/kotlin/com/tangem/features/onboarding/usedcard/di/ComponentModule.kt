package com.tangem.features.onboarding.usedcard.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.usedcard.UsedCardOnboardingComponent
import com.tangem.features.onboarding.usedcard.alreadyactivated.AlreadyActivatedModel
import com.tangem.features.onboarding.usedcard.entry.DefaultUsedCardOnboardingComponent
import com.tangem.features.onboarding.usedcard.entry.UsedCardOnboardingModel
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
    fun bindUsedCardOnboardingComponent(
        factory: DefaultUsedCardOnboardingComponent.Factory,
    ): UsedCardOnboardingComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(UsedCardOnboardingModel::class)
    fun provideUsedCardOnboardingModel(model: UsedCardOnboardingModel): Model

    @Binds
    @IntoMap
    @ClassKey(AlreadyActivatedModel::class)
    fun provideAlreadyActivatedModel(model: AlreadyActivatedModel): Model
}