package com.tangem.features.onramp.swap.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.swap.model.SwapSelectTokensModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface SwapSelectTokensModelModule {

    @Binds
    @IntoMap
    @ClassKey(SwapSelectTokensModel::class)
    fun bindOnrampTokenListModel(model: SwapSelectTokensModel): Model
}