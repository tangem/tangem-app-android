package com.tangem.features.yield.supply.impl.subcomponents.stopearning.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.model.YieldSupplyStopEarningModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyStopEarningModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyStopEarningModel::class)
    fun provideYieldSupplyStopEarningModel(impl: YieldSupplyStopEarningModel): Model
}