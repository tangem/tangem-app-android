package com.tangem.features.onramp.swap.di

import com.tangem.features.onramp.component.SwapSelectTokensComponent
import com.tangem.features.onramp.swap.DefaultSwapSelectTokensComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapSelectTokensComponentModule {

    @Binds
    @Singleton
    fun bindSwapSelectTokensComponentFactory(
        factory: DefaultSwapSelectTokensComponent.Factory,
    ): SwapSelectTokensComponent.Factory
}