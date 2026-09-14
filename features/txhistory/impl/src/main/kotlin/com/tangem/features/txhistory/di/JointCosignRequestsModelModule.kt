package com.tangem.features.txhistory.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.txhistory.joint.requests.model.JointCosignRequestsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface JointCosignRequestsModelModule {
    @Binds
    @IntoMap
    @ClassKey(JointCosignRequestsModel::class)
    fun bindModel(model: JointCosignRequestsModel): Model
}