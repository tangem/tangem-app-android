package com.tangem.features.foryou.impl.tokensummary.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.DefaultTokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.model.TokenSummaryModel
import com.tangem.features.foryou.impl.tokensummary.swapchooser.model.SwapTokenChooserModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenSummaryComponentModule {

    @Binds
    @Singleton
    fun bindTokenSummaryComponent(factory: DefaultTokenSummaryComponent.Factory): TokenSummaryComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(TokenSummaryModel::class)
    fun bindTokenSummaryModel(impl: TokenSummaryModel): Model

    @Binds
    @IntoMap
    @ClassKey(SwapTokenChooserModel::class)
    fun bindSwapTokenChooserModel(impl: SwapTokenChooserModel): Model
}