package com.tangem.features.markets.tokenlist.impl.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.markets.tokenlist.impl.model.MarketsListModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(MarketsListModel::class)
    fun provideMarketsListModel(model: MarketsListModel): Model
}
