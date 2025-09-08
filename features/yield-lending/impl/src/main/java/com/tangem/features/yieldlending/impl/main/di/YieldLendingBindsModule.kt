package com.tangem.features.yieldlending.impl.main.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yieldlending.api.YieldLendingComponent
import com.tangem.features.yieldlending.impl.main.DefaultYieldLendingComponent
import com.tangem.features.yieldlending.impl.main.model.YieldLendingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldLendingBindsModule {
    @Binds
    @Singleton
    fun provideYieldLendingComponentFactory(impl: DefaultYieldLendingComponent.Factory): YieldLendingComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldLendingModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldLendingModel::class)
    fun provideYieldLendingModel(impl: YieldLendingModel): Model
}