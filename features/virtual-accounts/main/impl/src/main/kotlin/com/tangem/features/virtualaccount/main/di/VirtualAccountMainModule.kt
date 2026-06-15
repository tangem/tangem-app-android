package com.tangem.features.virtualaccount.main.di

import com.tangem.features.virtualaccount.main.component.DefaultVirtualAccountMainBlockComponent
import com.tangem.features.virtualaccount.main.component.VirtualAccountMainBlockComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface VirtualAccountMainModule {
    @Binds
    fun bindVirtualAccountMainBlockComponent(
        factory: DefaultVirtualAccountMainBlockComponent.Factory,
    ): VirtualAccountMainBlockComponent.Factory
}