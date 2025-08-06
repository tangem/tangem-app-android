package com.tangem.features.hotwallet.stepper.di

import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.hotwallet.stepper.impl.DefaultHotWalletStepperComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface HotWalletStepperModule {

    @Binds
    fun bindHotWalletStepperComponentFactory(
        impl: DefaultHotWalletStepperComponent.Factory,
    ): HotWalletStepperComponent.Factory
}