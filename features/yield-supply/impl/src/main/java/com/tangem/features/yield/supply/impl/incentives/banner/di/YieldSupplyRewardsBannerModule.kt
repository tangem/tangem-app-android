package com.tangem.features.yield.supply.impl.incentives.banner.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.incentives.banner.model.YieldSupplyRewardsBannerModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyRewardsBannerModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyRewardsBannerModel::class)
    fun bindYieldSupplyRewardsBannerModel(impl: YieldSupplyRewardsBannerModel): Model
}