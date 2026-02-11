package com.tangem.features.yield.supply.impl.entry.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.api.YieldSupplyEntryComponent
import com.tangem.features.yield.supply.impl.entry.DefaultYieldSupplyEntryComponent
import com.tangem.features.yield.supply.impl.entry.model.YieldSupplyEntryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyEntryBindsModule {
    @Binds
    @Singleton
    fun provideYieldSupplyEntryComponentFactory(
        impl: DefaultYieldSupplyEntryComponent.Factory,
    ): YieldSupplyEntryComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyEntryModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyEntryModel::class)
    fun provideYieldSupplyEntryModel(impl: YieldSupplyEntryModel): Model
}