package com.tangem.features.send.v2.entrypoint.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.entrypoint.model.SendEntryPointModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface SendEntryPointModule {

    @Binds
    @IntoMap
    @ClassKey(SendEntryPointModel::class)
    fun provideSendEntryPointModel(model: SendEntryPointModel): Model
}