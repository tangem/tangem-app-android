package com.tangem.data.tokens.di

import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.tokens.AccountListCryptoCurrenciesFetcher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class MultiWalletAccountListFetcherModule {

    @Singleton
    @Provides
    fun provideMultiWalletAccountListFetcher(
        userWalletsListRepository: UserWalletsListRepository,
        walletAccountsFetcher: WalletAccountsFetcher,
        dispatchers: CoroutineDispatcherProvider,
    ): MultiWalletAccountListFetcher {
        return AccountListCryptoCurrenciesFetcher(
            userWalletsListRepository = userWalletsListRepository,
            walletAccountsFetcher = walletAccountsFetcher,
            dispatchers = dispatchers,
        )
    }
}