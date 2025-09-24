package com.tangem.features.yield.supply.impl.subcomponents.approve.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.subcomponents.approve.model.YieldSupplyApproveModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyApproveModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyApproveModel::class)
    fun provideYieldSupplyApproveModel(impl: YieldSupplyApproveModel): Model
}