package com.tangem.features.markets.portfolio.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.markets.portfolio.impl.model.MarketsPortfolioModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(MarketsPortfolioModel::class)
    fun provideMarketsPortfolioModel(model: MarketsPortfolioModel): Model
}