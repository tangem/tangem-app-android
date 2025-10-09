package com.tangem.features.account.di

import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.account.fetcher.DefaultPortfolioFetcher
import com.tangem.features.account.selector.DefaultPortfolioSelectorComponent
import com.tangem.features.account.selector.DefaultPortfolioSelectorController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountFeatureModule {

    @Binds
    fun bindPortfolioFetcherFactory(impl: DefaultPortfolioFetcher.Factory): PortfolioFetcher.Factory

    @Binds
    fun bindPortfolioSelectorController(impl: DefaultPortfolioSelectorController): PortfolioSelectorController

    @Binds
    fun bindPortfolioSelectorComponentFactory(
        impl: DefaultPortfolioSelectorComponent.Factory,
    ): PortfolioSelectorComponent.Factory
}