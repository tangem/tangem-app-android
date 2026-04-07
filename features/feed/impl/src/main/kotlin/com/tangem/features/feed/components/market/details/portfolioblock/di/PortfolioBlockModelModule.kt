package com.tangem.features.feed.components.market.details.portfolioblock.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.components.market.details.portfolioblock.model.PortfolioBlockModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface PortfolioBlockModelModule {

    @Binds
    @IntoMap
    @ClassKey(PortfolioBlockModel::class)
    fun providePortfolioBlockModel(model: PortfolioBlockModel): Model
}