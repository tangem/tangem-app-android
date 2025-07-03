package com.tangem.features.swap.v2.impl.amount.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.features.swap.v2.impl.amount.DefaultSwapAmountUpdateTrigger
import com.tangem.features.swap.v2.impl.amount.SwapAmountUpdateListener
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

@InstallIn(SingletonComponent::class)
@Module
internal interface SwapAmountModuleBinds {

    @Binds
    fun provideSwapAmountUpdateTrigger(impl: DefaultSwapAmountUpdateTrigger): SwapAmountUpdateTrigger

    @Binds
    fun provideSwapAmountUpdateListener(impl: DefaultSwapAmountUpdateTrigger): SwapAmountUpdateListener
}