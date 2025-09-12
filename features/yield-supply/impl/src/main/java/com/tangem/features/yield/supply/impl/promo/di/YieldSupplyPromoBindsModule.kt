package com.tangem.features.yield.supply.impl.promo.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.promo.DefaultYieldSupplyPromoComponent
import com.tangem.features.yield.supply.impl.promo.model.YieldSupplyPromoModel
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyPromoBindsModule {
    @Binds
    @Singleton
    fun provideYieldSupplyPromoComponentFactory(
        impl: DefaultYieldSupplyPromoComponent.Factory,
    ): YieldSupplyPromoComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyPromoModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyPromoModel::class)
    fun provideYieldSupplyPromoModel(impl: YieldSupplyPromoModel): Model
}