package com.tangem.features.virtualaccount.main.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.virtualaccount.main.VirtualAccountMainModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface VirtualAccountMainModelModule {

    @Binds
    @IntoMap
    @ClassKey(VirtualAccountMainModel::class)
    fun bindVirtualAccountMainModel(model: VirtualAccountMainModel): Model
}