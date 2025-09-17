package com.tangem.features.yield.supply.impl.subcomponents.startearning.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.subcomponents.startearning.model.YieldSupplyStartEarningEntryModel
import com.tangem.features.yield.supply.impl.subcomponents.startearning.model.YieldSupplyStartEarningModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyStartEarningModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyStartEarningModel::class)
    fun provideYieldSupplyStartEarningModel(impl: YieldSupplyStartEarningModel): Model

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyStartEarningEntryModel::class)
    fun provideYieldSupplyStartEarningEntryModel(impl: YieldSupplyStartEarningEntryModel): Model
}