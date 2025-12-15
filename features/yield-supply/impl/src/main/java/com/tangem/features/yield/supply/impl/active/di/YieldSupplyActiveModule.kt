package com.tangem.features.yield.supply.impl.active.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.active.DefaultYieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.active.model.YieldSupplyActiveModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyActiveBindsModule {
    @Binds
    @Singleton
    fun provideYieldSupplyActiveComponentFactory(
        impl: DefaultYieldSupplyActiveComponent.Factory,
    ): YieldSupplyActiveComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyActiveModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyActiveModel::class)
    fun provideYieldSupplyActiveModel(impl: YieldSupplyActiveModel): Model
}