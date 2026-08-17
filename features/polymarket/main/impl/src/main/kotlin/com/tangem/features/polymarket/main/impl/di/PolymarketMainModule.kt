package com.tangem.features.polymarket.main.impl.di

import com.tangem.features.polymarket.main.api.PolymarketMainBlockComponent
import com.tangem.features.polymarket.main.impl.component.DefaultPolymarketMainBlockComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketMainModule {

    @Binds
    fun bindPolymarketMainBlockComponent(
        factory: DefaultPolymarketMainBlockComponent.Factory,
    ): PolymarketMainBlockComponent.Factory
}