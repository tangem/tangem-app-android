package com.tangem.features.markets.details.impl.di

import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.DefaultMarketsTokenDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindMarketsTokenDetailsComponent(
        factory: DefaultMarketsTokenDetailsComponent.Factory,
    ): MarketsTokenDetailsComponent.Factory
}
