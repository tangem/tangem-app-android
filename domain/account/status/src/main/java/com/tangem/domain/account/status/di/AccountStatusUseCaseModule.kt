package com.tangem.domain.account.status.di

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.*
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountStatusUseCaseModule {

    @Provides
    @Singleton
    fun provideGetAccountCurrencyByAddressUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        singleAccountListSupplier: SingleAccountListSupplier,
    ): GetAccountCurrencyByAddressUseCase {
        return GetAccountCurrencyByAddressUseCase(
            accountsCRUDRepository = accountsCRUDRepository,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            singleAccountListSupplier = singleAccountListSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrencyActionsUseCaseV2(
        accountsCRUDRepository: AccountsCRUDRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    ): GetCryptoCurrencyActionsUseCaseV2 {
        return GetCryptoCurrencyActionsUseCaseV2(
            accountsCRUDRepository = accountsCRUDRepository,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideApplyTokenListSortingUseCaseV2(
        accountsCRUDRepository: AccountsCRUDRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCaseV2 {
        return ApplyTokenListSortingUseCaseV2(
            accountsCRUDRepository = accountsCRUDRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetWalletTotalBalanceUseCaseV2(
        multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    ): GetWalletTotalBalanceUseCaseV2 {
        return GetWalletTotalBalanceUseCaseV2(
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
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
    fun provideManageCryptoCurrenciesUseCase(
        singleAccountListSupplier: SingleAccountListSupplier,
        accountsCRUDRepository: AccountsCRUDRepository,
        currenciesRepository: CurrenciesRepository,
        derivationsRepository: DerivationsRepository,
        cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
        networksCleaner: NetworksCleaner,
        stakingCleaner: StakingCleaner,
        expressServiceFetcher: ExpressServiceFetcher,
        dispatchers: CoroutineDispatcherProvider,
    ): ManageCryptoCurrenciesUseCase {
        return ManageCryptoCurrenciesUseCase(
            singleAccountListSupplier = singleAccountListSupplier,
            accountsCRUDRepository = accountsCRUDRepository,
            currenciesRepository = currenciesRepository,
            derivationsRepository = derivationsRepository,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            networksCleaner = networksCleaner,
            stakingCleaner = stakingCleaner,
            expressServiceFetcher = expressServiceFetcher,
            parallelUpdatingScope = CoroutineScope(SupervisorJob() + dispatchers.default),
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideCryptoCurrencyBalanceFetcher(
        accountsCRUDRepository: AccountsCRUDRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): CryptoCurrencyBalanceFetcher {
        return CryptoCurrencyBalanceFetcher(
            accountsCRUDRepository = accountsCRUDRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            parallelUpdatingScope = CoroutineScope(SupervisorJob() + dispatchers.default),
        )
    }

    @Provides
    @Singleton
    fun provideToggleTokenListSortingUseCaseV2(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListSortingUseCaseV2 {
        return ToggleTokenListSortingUseCaseV2(
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideToggleTokenListGroupingUseCaseV2(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListGroupingUseCaseV2 {
        return ToggleTokenListGroupingUseCaseV2(
            dispatchers = dispatchers,
        )
    }
}