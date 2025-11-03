package com.tangem.domain.account.status.di

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyByAddressUseCase
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.SaveCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountStatusUseCaseModule {

    @Provides
    @Singleton
    fun provideGetAccountCurrencyByAddressUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        singleAccountListSupplier: SingleAccountListSupplier,
    ): GetAccountCurrencyByAddressUseCase {
        return GetAccountCurrencyByAddressUseCase(
            userWalletsListRepository = userWalletsListRepository,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            singleAccountListSupplier = singleAccountListSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetAccountCurrencyStatusUseCase(
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    ): GetAccountCurrencyStatusUseCase {
        return GetAccountCurrencyStatusUseCase(singleAccountStatusListSupplier = singleAccountStatusListSupplier)
    }

    @Provides
    @Singleton
    fun provideSaveCryptoCurrenciesUseCase(
        singleAccountListSupplier: SingleAccountListSupplier,
        accountsCRUDRepository: AccountsCRUDRepository,
        currenciesRepository: CurrenciesRepository,
        derivationsRepository: DerivationsRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
        networksCleaner: NetworksCleaner,
        stakingCleaner: StakingCleaner,
        dispatchers: CoroutineDispatcherProvider,
    ): SaveCryptoCurrenciesUseCase {
        return SaveCryptoCurrenciesUseCase(
            singleAccountListSupplier = singleAccountListSupplier,
            accountsCRUDRepository = accountsCRUDRepository,
            currenciesRepository = currenciesRepository,
            derivationsRepository = derivationsRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            networksCleaner = networksCleaner,
            stakingCleaner = stakingCleaner,
            dispatchers = dispatchers,
        )
    }
}