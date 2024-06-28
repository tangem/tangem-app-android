package com.tangem.data.walletconnect.di

import com.tangem.data.walletconnect.DefaultWalletConnectRepository
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectDataModule {

    @Provides
    @Singleton
    fun providesWalletConnectRepository(
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletConnectRepository {
        return DefaultWalletConnectRepository(userWalletsStore, dispatchers)
    }
}