package com.tangem.features.jointaccount.main.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.jointaccount.main.model.JointAccountMembersModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface JointAccountMainModelsModule {

    @Binds
    @IntoMap
    @ClassKey(JointAccountMembersModel::class)
    fun bindJointAccountMembersModel(model: JointAccountMembersModel): Model
}