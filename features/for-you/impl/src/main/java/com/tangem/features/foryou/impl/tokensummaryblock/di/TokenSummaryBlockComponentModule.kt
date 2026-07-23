package com.tangem.features.foryou.impl.tokensummaryblock.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.foryou.TokenSummaryBlockComponent
import com.tangem.features.foryou.impl.tokensummaryblock.DefaultTokenSummaryBlockComponent
import com.tangem.features.foryou.impl.tokensummaryblock.model.TokenSummaryBlockModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenSummaryBlockComponentModule {

    @Binds
    @Singleton
    fun bindTokenSummaryBlockComponent(
        factory: DefaultTokenSummaryBlockComponent.Factory,
    ): TokenSummaryBlockComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(TokenSummaryBlockModel::class)
    fun bindTokenSummaryBlockModel(impl: TokenSummaryBlockModel): Model
}