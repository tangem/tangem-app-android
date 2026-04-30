package com.tangem.features.commonfeatures.impl.portfolioselector.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.commonfeatures.impl.portfolioselector.DefaultPortfolioSelectorComponent
import com.tangem.features.commonfeatures.impl.portfolioselector.DefaultPortfolioSelectorController
import com.tangem.features.commonfeatures.impl.portfolioselector.PortfolioSelectorModel
import com.tangem.features.commonfeatures.impl.portfolioselector.fetcher.DefaultPortfolioFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface PortfolioSelectorModule {

    @Binds
    @IntoMap
    @ClassKey(PortfolioSelectorModel::class)
    fun portfolioSelectorModel(model: PortfolioSelectorModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal interface PortfolioSelectorSingletonModule {

    @Binds
    fun bindPortfolioFetcherFactory(impl: DefaultPortfolioFetcher.Factory): PortfolioFetcher.Factory

    @Binds
    fun bindPortfolioSelectorController(impl: DefaultPortfolioSelectorController): PortfolioSelectorController

    @Binds
    fun bindPortfolioSelectorComponentFactory(
        impl: DefaultPortfolioSelectorComponent.Factory,
    ): PortfolioSelectorComponent.Factory
}