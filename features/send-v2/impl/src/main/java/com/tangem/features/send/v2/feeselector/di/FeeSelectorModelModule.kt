package com.tangem.features.send.v2.feeselector.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.feeselector.component.extended.model.FeeExtendedSelectorModel
import com.tangem.features.send.v2.feeselector.model.FeeSelectorBlockModel
import com.tangem.features.send.v2.feeselector.component.speed.model.FeeSpeedSelectorModel
import com.tangem.features.send.v2.feeselector.component.token.model.FeeTokenSelectorModel
import com.tangem.features.send.v2.feeselector.model.FeeSelectorModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface FeeSelectorModelModule {
    @Binds
    @IntoMap
    @ClassKey(FeeSelectorModel::class)
    fun bindModel(model: FeeSelectorModel): Model

    @Binds
    @IntoMap
    @ClassKey(FeeExtendedSelectorModel::class)
    fun bindExtendedModel(model: FeeExtendedSelectorModel): Model

    @Binds
    @IntoMap
    @ClassKey(FeeSelectorBlockModel::class)
    fun bindBlockModel(model: FeeSelectorBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(FeeSpeedSelectorModel::class)
    fun bindFeeSpeedSelectorModel(model: FeeSpeedSelectorModel): Model

    @Binds
    @IntoMap
    @ClassKey(FeeTokenSelectorModel::class)
    fun bindFeeTokenSelectorModel(model: FeeTokenSelectorModel): Model
}