package com.tangem.feature.walletsettings.di

import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.component.impl.DefaultRenameWalletComponent
import com.tangem.feature.walletsettings.component.impl.DefaultWalletSettingsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindWalletSettingsComponentFactory(
        factory: DefaultWalletSettingsComponent.Factory,
    ): WalletSettingsComponent.Factory

    @Binds
    @Singleton
    fun bindRenameWalletComponentFactory(factory: DefaultRenameWalletComponent.Factory): RenameWalletComponent.Factory
}
