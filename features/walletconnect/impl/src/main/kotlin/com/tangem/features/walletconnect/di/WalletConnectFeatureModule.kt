package com.tangem.features.walletconnect.di

import com.tangem.features.walletconnect.components.WalletConnectEntryComponent
import com.tangem.features.walletconnect.connections.components.DefaultWalletConnectEntryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletConnectFeatureModule {

    @Binds
    @Singleton
    fun bindWalletConnectEntryComponentFactory(
        factory: DefaultWalletConnectEntryComponent.Factory,
    ): WalletConnectEntryComponent.Factory
}