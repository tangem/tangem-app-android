package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.component.DefaultTangemPayMainBlockComponent
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayMainModule {
    @Binds
    fun bindTangemPayMainBlockComponent(
        factory: DefaultTangemPayMainBlockComponent.Factory,
    ): TangemPayMainBlockComponent.Factory
}