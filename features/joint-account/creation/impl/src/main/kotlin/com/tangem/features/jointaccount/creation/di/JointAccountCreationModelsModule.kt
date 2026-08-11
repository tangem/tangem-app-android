package com.tangem.features.jointaccount.creation.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.jointaccount.creation.promo.model.JointAccountPromoModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface JointAccountCreationModelsModule {

    @Binds
    @IntoMap
    @ClassKey(JointAccountPromoModel::class)
    fun bindJointAccountPromoModel(model: JointAccountPromoModel): Model
}