package com.tangem.features.jointaccount.common.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.jointaccount.common.displayname.model.JointAccountDisplayNameModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface JointAccountCommonModelsModule {

    @Binds
    @IntoMap
    @ClassKey(JointAccountDisplayNameModel::class)
    fun bindJointAccountDisplayNameModel(model: JointAccountDisplayNameModel): Model
}