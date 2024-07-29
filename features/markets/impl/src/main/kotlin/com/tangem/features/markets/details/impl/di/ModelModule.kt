package com.tangem.features.markets.details.impl.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.markets.details.impl.model.MarketsTokenDetailsModel
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
    @ClassKey(MarketsTokenDetailsModel::class)
    fun provideMarketsTokenDetailsModel(model: MarketsTokenDetailsModel): Model
}