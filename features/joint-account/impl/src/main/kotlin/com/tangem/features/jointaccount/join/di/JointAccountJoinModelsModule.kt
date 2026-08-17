package com.tangem.features.jointaccount.join.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.jointaccount.join.invitepreview.model.JointAccountInvitePreviewModel
import com.tangem.features.jointaccount.join.model.JointAccountJoinModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface JointAccountJoinModelsModule {

    @Binds
    @IntoMap
    @ClassKey(JointAccountInvitePreviewModel::class)
    fun bindJointAccountInvitePreviewModel(model: JointAccountInvitePreviewModel): Model

    @Binds
    @IntoMap
    @ClassKey(JointAccountJoinModel::class)
    fun bindJointAccountJoinModel(model: JointAccountJoinModel): Model
}