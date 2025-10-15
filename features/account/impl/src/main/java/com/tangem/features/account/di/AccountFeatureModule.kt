package com.tangem.features.account.di

import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.account.archived.DefaultArchivedAccountListComponent
import com.tangem.features.account.createedit.DefaultAccountCreateEditComponent
import com.tangem.features.account.details.DefaultAccountDetailsComponent
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

    @Binds
    fun bindAccountCreateEditComponentFactory(
        impl: DefaultAccountCreateEditComponent.Factory,
    ): AccountCreateEditComponent.Factory

    @Binds
    fun bindAccountDetailsComponentFactory(
        impl: DefaultAccountDetailsComponent.Factory,
    ): AccountDetailsComponent.Factory

    @Binds
    fun bindArchivedAccountListComponentFactory(
        impl: DefaultArchivedAccountListComponent.Factory,
    ): ArchivedAccountListComponent.Factory
}