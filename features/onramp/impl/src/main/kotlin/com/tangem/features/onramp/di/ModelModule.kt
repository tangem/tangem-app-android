package com.tangem.features.onramp.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.model.OnrampModel
import com.tangem.features.onramp.model.ResidenceModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampModel::class)
    fun provideOnrampModel(model: OnrampModel): Model

    @Binds
    @IntoMap
    @ClassKey(ResidenceModel::class)
    fun provideResidenceModel(model: ResidenceModel): Model
}
