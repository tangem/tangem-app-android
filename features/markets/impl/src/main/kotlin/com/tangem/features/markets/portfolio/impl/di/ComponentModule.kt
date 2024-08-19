package com.tangem.features.markets.portfolio.impl.di

import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.DefaultMarketsPortfolioComponent
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
        factory: DefaultMarketsPortfolioComponent.Factory,
    ): MarketsPortfolioComponent.Factory
}