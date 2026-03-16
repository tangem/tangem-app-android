package com.tangem.features.send.v2.networkselection.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.networkselection.model.NetworkSelectionModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface NetworkSelectionModelModule {

    @Binds
    @IntoMap
    @ClassKey(NetworkSelectionModel::class)
    fun provideNetworkSelectionModel(model: NetworkSelectionModel): Model
}