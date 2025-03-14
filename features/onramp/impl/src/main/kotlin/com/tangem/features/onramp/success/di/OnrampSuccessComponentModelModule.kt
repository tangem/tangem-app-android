package com.tangem.features.onramp.success.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.success.model.OnrampSuccessComponentModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface OnrampSuccessComponentModelModule {
    @Binds
    @IntoMap
    @ClassKey(OnrampSuccessComponentModel::class)
    fun bindOnrampSuccessComponentModel(model: OnrampSuccessComponentModel): Model
}