package com.tangem.features.yield.supply.impl.main.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.DefaultYieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.model.YieldSupplyModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyBindsModule {
    @Binds
    @Singleton
    fun provideYieldSupplyComponentFactory(impl: DefaultYieldSupplyComponent.Factory): YieldSupplyComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyModel::class)
    fun provideYieldSupplyModel(impl: YieldSupplyModel): Model
}