package com.tangem.features.yield.supply.impl.warning.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.api.YieldSupplyDepositedWarningComponent
import com.tangem.features.yield.supply.impl.warning.DefaultYieldSupplyDepositedWarningComponent
import com.tangem.features.yield.supply.impl.warning.model.YieldSupplyDepositedWarningModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyDepositedWarningModule {
    @Binds
    @Singleton
    fun provideYieldSupplyWarningComponentFactory(
        impl: DefaultYieldSupplyDepositedWarningComponent.Factory,
    ): YieldSupplyDepositedWarningComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyDepositedWarningModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyDepositedWarningModel::class)
    fun provideYieldSupplyWarningModel(impl: YieldSupplyDepositedWarningModel): Model
}