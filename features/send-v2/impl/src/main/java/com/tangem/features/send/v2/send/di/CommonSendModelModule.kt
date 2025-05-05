package com.tangem.features.send.v2.send.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.send.confirm.model.SendConfirmModel
import com.tangem.features.send.v2.send.model.SendModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface CommonSendModelModule {

    @Binds
    @IntoMap
    @ClassKey(SendModel::class)
    fun provideSendModel(model: SendModel): Model

    @Binds
    @IntoMap
    @ClassKey(SendConfirmModel::class)
    fun provideSendConfirmModel(model: SendConfirmModel): Model
}