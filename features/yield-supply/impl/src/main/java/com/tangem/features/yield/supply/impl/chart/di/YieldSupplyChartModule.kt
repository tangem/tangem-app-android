package com.tangem.features.yield.supply.impl.chart.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.chart.model.YieldSupplyChartModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyChartModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyChartModel::class)
    fun bindYieldSupplyChartModel(impl: YieldSupplyChartModel): Model
}