package com.tangem.features.account.selector.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.account.selector.PortfolioSelectorModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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