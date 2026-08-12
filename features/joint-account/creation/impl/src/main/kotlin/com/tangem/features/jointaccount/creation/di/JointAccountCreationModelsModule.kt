package com.tangem.features.jointaccount.creation.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.jointaccount.creation.composition.model.JointAccountCompositionModel
import com.tangem.features.jointaccount.creation.config.model.JointAccountConfigModel
import com.tangem.features.jointaccount.creation.model.JointAccountCreationModel
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

    @Binds
    @IntoMap
    @ClassKey(JointAccountConfigModel::class)
    fun bindJointAccountConfigModel(model: JointAccountConfigModel): Model

    @Binds
    @IntoMap
    @ClassKey(JointAccountCompositionModel::class)
    fun bindJointAccountCompositionModel(model: JointAccountCompositionModel): Model

    @Binds
    @IntoMap
    @ClassKey(JointAccountCreationModel::class)
    fun bindJointAccountCreationModel(model: JointAccountCreationModel): Model
}