package com.tangem.features.feed.components.market.details.portfolio.add.impl.di

import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager
import com.tangem.features.feed.components.market.details.portfolio.add.impl.DefaultAddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.ui.DefaultAddToPortfolioManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AddToPortfolioComponentModule {

    @Binds
    fun bindAddToPortfolioComponent(factory: DefaultAddToPortfolioComponent.Factory): AddToPortfolioComponent.Factory

    @Binds
    fun bindAddToPortfolioManagerFactory(factory: DefaultAddToPortfolioManager.Factory): AddToPortfolioManager.Factory
}