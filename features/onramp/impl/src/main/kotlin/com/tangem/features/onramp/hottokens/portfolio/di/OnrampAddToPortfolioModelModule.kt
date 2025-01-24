package com.tangem.features.onramp.hottokens.portfolio.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.hottokens.portfolio.model.OnrampAddToPortfolioModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface OnrampAddToPortfolioModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampAddToPortfolioModel::class)
    fun bindOnrampSelectCountryModel(model: OnrampAddToPortfolioModel): Model
}