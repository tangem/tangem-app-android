package com.tangem.features.jointaccount.supportednetworks.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.jointaccount.supportednetworks.model.JointSupportedNetworksModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface JointSupportedNetworksModelsModule {

    @Binds
    @IntoMap
    @ClassKey(JointSupportedNetworksModel::class)
    fun bindJointSupportedNetworksModel(model: JointSupportedNetworksModel): Model
}