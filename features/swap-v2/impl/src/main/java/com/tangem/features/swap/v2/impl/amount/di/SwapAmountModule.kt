package com.tangem.features.swap.v2.impl.amount.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@InstallIn(ModelComponent::class)
@Module
internal interface SwapAmountModule {

    @Binds
    @IntoMap
    @ClassKey(SwapAmountModel::class)
    fun provideSwapAmountModel(impl: SwapAmountModel): Model
}