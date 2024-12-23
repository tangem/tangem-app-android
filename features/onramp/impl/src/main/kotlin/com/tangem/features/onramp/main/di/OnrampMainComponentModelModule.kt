package com.tangem.features.onramp.main.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.main.model.OnrampMainComponentModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface OnrampMainComponentModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampMainComponentModel::class)
    fun bindOnrampSelectCountryModel(model: OnrampMainComponentModel): Model
}