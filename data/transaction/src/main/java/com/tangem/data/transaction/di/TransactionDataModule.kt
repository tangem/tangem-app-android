package com.tangem.data.transaction.di

import com.tangem.data.transaction.DefaultFeeRepository
import com.tangem.data.transaction.DefaultTransactionRepository
import com.tangem.data.transaction.DefaultWalletAddressServiceRepository
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TransactionDataModule {

    @Provides
    @Singleton
    fun providesTransactionRepository(
        walletManagersFacade: WalletManagersFacade,
        walletManagersStore: WalletManagersStore,
        dispatchers: CoroutineDispatcherProvider,
    ): TransactionRepository {
        return DefaultTransactionRepository(
            walletManagersFacade = walletManagersFacade,
            walletManagersStore = walletManagersStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun providesFeeRepository(): FeeRepository {
        return DefaultFeeRepository()
    }

    @Provides
    @Singleton
    fun providesWalletAddressServiceRepository(
        walletManagersFacade: WalletManagersFacade,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): WalletAddressServiceRepository {
        return DefaultWalletAddressServiceRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = coroutineDispatcherProvider,
        )
    }
}