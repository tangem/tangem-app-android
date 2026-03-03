package com.tangem.data.tokens.di

import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.tokens.AccountListCryptoCurrenciesFetcher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class MultiWalletCryptoCurrenciesFetcherModule {

    @Singleton
    @Provides
    fun provideMultiWalletCryptoCurrenciesFetcher(
        userWalletsListRepository: UserWalletsListRepository,
        walletAccountsFetcher: WalletAccountsFetcher,
        expressServiceFetcher: ExpressServiceFetcher,
        dispatchers: CoroutineDispatcherProvider,
    ): MultiWalletCryptoCurrenciesFetcher {
        return AccountListCryptoCurrenciesFetcher(
            userWalletsListRepository = userWalletsListRepository,
            walletAccountsFetcher = walletAccountsFetcher,
            expressServiceFetcher = expressServiceFetcher,
            dispatchers = dispatchers,
        )
    }
}