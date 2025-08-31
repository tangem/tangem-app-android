package com.tangem.features.tangempay.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.components.DefaultTangemPayOnboardingComponent
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.model.TangemPayOnboardingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayOnboardingFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultTangemPayOnboardingComponent.Factory): TangemPayOnboardingComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(TangemPayOnboardingModel::class)
    fun bindModel(model: TangemPayOnboardingModel): Model
}