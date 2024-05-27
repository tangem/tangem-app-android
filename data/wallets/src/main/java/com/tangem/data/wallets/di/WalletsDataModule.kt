package com.tangem.data.wallets.di

import com.tangem.data.wallets.DefaultWalletNamesMigrationRepository
import com.tangem.data.wallets.DefaultWalletAddressServiceRepository
import com.tangem.data.wallets.DefaultWalletsRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletAddressServiceRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDataModule {

    @Provides
    @Singleton
    fun providesWalletsRepository(appPreferencesStore: AppPreferencesStore): WalletsRepository {
        return DefaultWalletsRepository(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun providesWalletAddressServiceRepository(
        walletManagersFacade: WalletManagersFacade,
    ): WalletAddressServiceRepository {
        return DefaultWalletAddressServiceRepository(walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideMigrateNamesRepository(appPreferencesStore: AppPreferencesStore): WalletNamesMigrationRepository {
        return DefaultWalletNamesMigrationRepository(appPreferencesStore)
    }
}
