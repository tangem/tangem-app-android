package com.tangem.features.onramp.hottokens.portfolio.di

import com.tangem.features.onramp.hottokens.portfolio.DefaultOnrampAddToPortfolioComponent
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampAddToPortfolioComponentModule {

    @Binds
    @Singleton
    fun bindOnrampAddToPortfolioComponentFactory(
        factory: DefaultOnrampAddToPortfolioComponent.Factory,
    ): OnrampAddToPortfolioComponent.Factory
}