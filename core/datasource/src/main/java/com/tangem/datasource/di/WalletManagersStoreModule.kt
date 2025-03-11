package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.walletmanager.DefaultWalletManagersStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletManagersStoreModule {

    @Provides
    @Singleton
    fun provideWalletManagersStore(): WalletManagersStore {
        return DefaultWalletManagersStore(
            dataStore = RuntimeDataStore(),
        )
    }
}
