package com.tangem.feature.walletsettings.di

import com.tangem.feature.walletsettings.component.AddAccountTypeComponent
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.component.impl.DefaultAddAccountTypeComponent
import com.tangem.feature.walletsettings.component.impl.DefaultNetworksAvailableForNotificationsComponent
import com.tangem.feature.walletsettings.component.impl.DefaultRenameWalletComponent
import com.tangem.feature.walletsettings.component.impl.DefaultWalletSettingsComponent
import com.tangem.feature.walletsettings.component.NetworksAvailableForNotificationsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletSettingsComponentModule {

    @Binds
    @Singleton
    fun bindWalletSettingsComponentFactory(
        factory: DefaultWalletSettingsComponent.Factory,
    ): WalletSettingsComponent.Factory

    @Binds
    @Singleton
    fun bindRenameWalletComponentFactory(factory: DefaultRenameWalletComponent.Factory): RenameWalletComponent.Factory

    @Binds
    @Singleton
    fun bindNetworksComponentFactory(
        factory: DefaultNetworksAvailableForNotificationsComponent.Factory,
    ): NetworksAvailableForNotificationsComponent.Factory

    @Binds
    @Singleton
    fun bindAddAccountTypeComponentFactory(
        factory: DefaultAddAccountTypeComponent.Factory,
    ): AddAccountTypeComponent.Factory
}