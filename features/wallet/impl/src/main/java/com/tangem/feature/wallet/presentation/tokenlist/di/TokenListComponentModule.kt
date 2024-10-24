package com.tangem.feature.wallet.presentation.tokenlist.di

import com.tangem.feature.wallet.presentation.tokenlist.DefaultTokenListComponent
import com.tangem.feature.wallet.presentation.tokenlist.TokenListComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenListComponentModule {

    @Binds
    @Singleton
    fun bindTokenListComponentFactory(factory: DefaultTokenListComponent.Factory): TokenListComponent.Factory
}