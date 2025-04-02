package com.tangem.features.onboarding.v2.twin.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.DefaultOnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.model.OnboardingTwinModel
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
    fun bindComponent(factory: DefaultOnboardingTwinComponent.Factory): OnboardingTwinComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnboardingTwinModel::class)
    fun provideModel(model: OnboardingTwinModel): Model
}