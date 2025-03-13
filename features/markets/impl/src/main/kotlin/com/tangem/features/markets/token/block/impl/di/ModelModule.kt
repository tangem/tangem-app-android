package com.tangem.features.markets.token.block.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.markets.token.block.impl.model.TokenMarketBlockModel
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
    @ClassKey(TokenMarketBlockModel::class)
    fun provideTokenMarketBlockModel(model: TokenMarketBlockModel): Model
}