package com.tangem.features.onramp.swap.availablepairs.di

import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.availablepairs.DefaultAvailableSwapPairsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AvailableSwapPairsComponentModule {

    @Binds
    @Singleton
    fun bindAvailableSwapPairsComponentFactory(
        factory: DefaultAvailableSwapPairsComponent.Factory,
    ): AvailableSwapPairsComponent.Factory
}