package com.tangem.data.walletmanager.di

import com.tangem.data.walletmanager.DefaultJointNetworkReader
import com.tangem.data.walletmanager.DefaultWalletManagersFacade
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletManagerModule {

    @Binds
    @Singleton
    fun bindWalletManagerFacade(impl: DefaultWalletManagersFacade): WalletManagersFacade

    @Binds
    @Singleton
    fun bindJointNetworkReader(impl: DefaultJointNetworkReader): JointNetworkReader
}