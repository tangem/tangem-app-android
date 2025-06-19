package com.tangem.features.swap.v2.impl.choosetoken.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkComponent
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkTrigger
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.DefaultSwapChooseTokenNetworkComponent
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.DefaultSwapChooseTokenNetworkTrigger
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.SwapChooseTokenNetworkListener
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenNetworkModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@InstallIn(ModelComponent::class)
@Module
internal interface SwapChooseTokenModule {

    @Binds
    @IntoMap
    @ClassKey(SwapChooseTokenNetworkModel::class)
    fun provideSwapChooseTokenNetworkModel(impl: SwapChooseTokenNetworkModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapChooseTokenModuleBinds {

    @Binds
    @Singleton
    fun provideSwapChooseTokenNetworksBottomSheetComponent(
        impl: DefaultSwapChooseTokenNetworkComponent.Factory,
    ): SwapChooseTokenNetworkComponent.Factory

    @Binds
    @Singleton
    fun bindSwapChooseTokenNetworkTrigger(impl: DefaultSwapChooseTokenNetworkTrigger): SwapChooseTokenNetworkTrigger

    @Binds
    @Singleton
    fun bindSwapChooseTokenNetworkListener(impl: DefaultSwapChooseTokenNetworkTrigger): SwapChooseTokenNetworkListener
}