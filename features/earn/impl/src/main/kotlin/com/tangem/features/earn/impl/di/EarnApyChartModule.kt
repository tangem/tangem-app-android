package com.tangem.features.earn.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.earn.impl.widgets.chart.model.EarnApyChartModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface EarnApyChartModule {

    @Binds
    @IntoMap
    @ClassKey(EarnApyChartModel::class)
    fun bindEarnApyChartModel(model: EarnApyChartModel): Model
}