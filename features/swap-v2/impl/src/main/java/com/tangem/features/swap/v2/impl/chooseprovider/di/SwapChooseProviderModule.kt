package com.tangem.features.swap.v2.impl.chooseprovider.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.impl.chooseprovider.model.SwapChooseProviderModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@InstallIn(ModelComponent::class)
@Module
internal interface SwapChooseProviderModule {

    @Binds
    @IntoMap
    @ClassKey(SwapChooseProviderModel::class)
    fun provideSwapChooseProviderModel(impl: SwapChooseProviderModel): Model
}