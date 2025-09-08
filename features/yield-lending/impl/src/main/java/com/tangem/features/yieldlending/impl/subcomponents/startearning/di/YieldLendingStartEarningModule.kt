package com.tangem.features.yieldlending.impl.subcomponents.startearning.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yieldlending.impl.subcomponents.startearning.model.YieldLendingActionModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface YieldLendingStartEarningModule {

    @Binds
    @IntoMap
    @ClassKey(YieldLendingActionModel::class)
    fun provideYieldLendingStartEarningModel(impl: YieldLendingActionModel): Model
}