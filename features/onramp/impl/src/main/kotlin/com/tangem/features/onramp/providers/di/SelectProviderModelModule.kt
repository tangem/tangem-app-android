package com.tangem.features.onramp.providers.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.providers.model.SelectProviderModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface SelectProviderModelModule {

    @Binds
    @IntoMap
    @ClassKey(SelectProviderModel::class)
    fun bindSelectProviderModel(model: SelectProviderModel): Model
}