package com.tangem.features.onramp.confirmresidency.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.confirmresidency.model.ConfirmResidencyModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface OnrampConfirmResidencyModelModule {
    @Binds
    @IntoMap
    @ClassKey(ConfirmResidencyModel::class)
    fun bindConfirmResidencyModel(model: ConfirmResidencyModel): Model
}