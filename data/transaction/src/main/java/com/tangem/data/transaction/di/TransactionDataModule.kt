package com.tangem.data.transaction.di

import com.tangem.data.transaction.DefaultTransactionRepository
import com.tangem.domain.transaction.TransactionRepository
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
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): TransactionRepository {
        return DefaultTransactionRepository(
            walletManagersFacade = walletManagersFacade,
            coroutineDispatcherProvider = coroutineDispatcherProvider,
        )
    }
}