package com.tangem.features.send.v2.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface SendModelModule {

    @Binds
    @IntoMap
    @ClassKey(SendDestinationModel::class)
    fun provideSendDestinationModel(model: SendDestinationModel): Model
}
