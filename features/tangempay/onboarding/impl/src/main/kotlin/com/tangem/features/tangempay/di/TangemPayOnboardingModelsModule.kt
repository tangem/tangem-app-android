package com.tangem.features.tangempay.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.model.TangemPayOnboardingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TangemPayOnboardingModelsModule {

    @Binds
    @IntoMap
    @ClassKey(TangemPayOnboardingModel::class)
    fun bindModel(model: TangemPayOnboardingModel): Model
}