package com.tangem.data.walletmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.tangem.data.walletmanager.DefaultWalletManagersFacade
import com.tangem.domain.walletmanager.WalletManagersFacade

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletManagerModule {

    @Binds
    @Singleton
    fun bindWalletManagerFacade(impl: DefaultWalletManagersFacade): WalletManagersFacade
}