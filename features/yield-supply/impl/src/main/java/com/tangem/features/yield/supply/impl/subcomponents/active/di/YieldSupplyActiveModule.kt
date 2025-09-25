package com.tangem.features.yield.supply.impl.subcomponents.active.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.subcomponents.active.model.YieldSupplyActiveEntryModel
import com.tangem.features.yield.supply.impl.subcomponents.active.model.YieldSupplyActiveModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyActiveModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyActiveModel::class)
    fun provideYieldSupplyActiveModel(impl: YieldSupplyActiveModel): Model

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyActiveEntryModel::class)
    fun provideYieldSupplyActiveEntryModel(impl: YieldSupplyActiveEntryModel): Model
}