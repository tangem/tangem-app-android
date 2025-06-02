package com.tangem.features.markets.tokenlist.impl.di

import com.tangem.features.markets.tokenlist.MarketsTokenListComponent
import com.tangem.features.markets.tokenlist.impl.DefaultMarketsTokenListComponent
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
    fun bindMarketsTokenListComponent(
        factory: DefaultMarketsTokenListComponent.Factory,
    ): MarketsTokenListComponent.Factory
}