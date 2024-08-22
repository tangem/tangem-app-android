package com.tangem.features.markets.token.block.impl.di

import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.markets.token.block.impl.DefaultTokenMarketBlockComponent
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
    fun bindMarketsPortfolioComponent(
        factory: DefaultTokenMarketBlockComponent.Factory,
    ): TokenMarketBlockComponent.Factory
}