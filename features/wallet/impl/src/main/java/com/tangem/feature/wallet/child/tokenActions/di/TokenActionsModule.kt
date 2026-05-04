package com.tangem.feature.wallet.child.tokenActions.di

import com.tangem.feature.wallet.child.tokenActions.DefaultTokenActionsComponent
import com.tangem.feature.wallet.child.tokenActions.TokenActionsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenActionsModule {

    @Binds
    fun bindTokenActionsComponentFactory(impl: DefaultTokenActionsComponent.Factory): TokenActionsComponent.Factory
}