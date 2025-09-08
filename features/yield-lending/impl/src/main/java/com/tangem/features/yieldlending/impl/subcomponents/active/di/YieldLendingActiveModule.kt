package com.tangem.features.yieldlending.impl.subcomponents.active.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yieldlending.impl.subcomponents.active.model.YieldLendingActiveModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldLendingActiveModule {

    @Binds
    @IntoMap
    @ClassKey(YieldLendingActiveModel::class)
    fun provideYieldLendingActiveModel(impl: YieldLendingActiveModel): Model
}