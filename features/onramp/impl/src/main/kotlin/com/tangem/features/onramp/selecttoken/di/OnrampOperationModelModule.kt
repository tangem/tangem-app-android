package com.tangem.features.onramp.selecttoken.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.selecttoken.model.OnrampOperationModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface OnrampOperationModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampOperationModel::class)
    fun bindOnrampOperationModel(model: OnrampOperationModel): Model
}