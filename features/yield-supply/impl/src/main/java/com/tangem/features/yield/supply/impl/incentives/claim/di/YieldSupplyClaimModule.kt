package com.tangem.features.yield.supply.impl.incentives.claim.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyClaimModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyClaimModel::class)
    fun provideYieldSupplyClaimModel(impl: YieldSupplyClaimModel): Model
}