package com.tangem.features.feed.components.market.details.portfolio.impl.di

import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.impl.DefaultMarketsPortfolioComponent
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