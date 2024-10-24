package com.tangem.feature.wallet.presentation.tokenlist.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.wallet.presentation.tokenlist.model.TokenListModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface TokenListModelModule {

    @Binds
    @IntoMap
    @ClassKey(TokenListModel::class)
    fun provideTokenListModel(model: TokenListModel): Model
}
