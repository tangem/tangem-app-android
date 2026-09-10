package com.tangem.data.walletmanager.di

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.data.walletmanager.store.DefaultWalletManagersStore
import com.tangem.data.walletmanager.store.WalletManagersStore
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
            store = RuntimeSharedMapStore(),
        )
    }
}