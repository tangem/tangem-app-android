package com.tangem.features.virtualaccount.onboarding.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.virtualaccount.onboarding.model.VirtualAccountOnboardingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface VirtualAccountOnboardingModelsModule {

    @Binds
    @IntoMap
    @ClassKey(VirtualAccountOnboardingModel::class)
    fun bindVirtualAccountOnboardingModel(model: VirtualAccountOnboardingModel): Model
}