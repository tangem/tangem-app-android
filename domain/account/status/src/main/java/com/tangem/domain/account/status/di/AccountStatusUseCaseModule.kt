package com.tangem.domain.account.status.di

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.*
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyMetadataCleaner
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.domain.tokens.BalanceFetchingOperations
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.AppCoroutineScope
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
    fun provideIsAccountsModeEnabledUseCase(
        multiAccountListSupplier: MultiAccountListSupplier,
        paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    ): IsAccountsModeEnabledUseCase {
        return IsAccountsModeEnabledUseCase(
            multiAccountListSupplier = multiAccountListSupplier,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrencyActionsUseCaseV2(
        userWalletsListRepository: UserWalletsListRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    ): GetCryptoCurrencyActionsUseCaseV2 {
        return GetCryptoCurrencyActionsUseCaseV2(
            userWalletsListRepository = userWalletsListRepository,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideApplyTokenListSortingUseCaseV2(
        accountsCRUDRepository: AccountsCRUDRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCase {
        return ApplyTokenListSortingUseCase(
            accountsCRUDRepository = accountsCRUDRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetWalletTotalBalanceUseCase(
        multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    ): GetWalletTotalBalanceUseCase {
        return GetWalletTotalBalanceUseCase(
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
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        accountsCRUDRepository: AccountsCRUDRepository,
        currenciesRepository: CurrenciesRepository,
        derivationsRepository: DerivationsRepository,
        walletManagersFacade: WalletManagersFacade,
        cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
        cryptoCurrencyMetadataCleaner: CryptoCurrencyMetadataCleaner,
        expressServiceFetcher: ExpressServiceFetcher,
        dispatchers: CoroutineDispatcherProvider,
        appScope: AppCoroutineScope,
    ): ManageCryptoCurrenciesUseCase {
        return ManageCryptoCurrenciesUseCase(
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            accountsCRUDRepository = accountsCRUDRepository,
            currenciesRepository = currenciesRepository,
            derivationsRepository = derivationsRepository,
            walletManagersFacade = walletManagersFacade,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
            cryptoCurrencyMetadataCleaner = cryptoCurrencyMetadataCleaner,
            expressServiceFetcher = expressServiceFetcher,
            parallelUpdatingScope = appScope,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideBalanceFetchingOperations(
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
    ): BalanceFetchingOperations {
        return BalanceFetchingOperations(
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
        )
    }

    @Provides
    @Singleton
    fun provideCryptoCurrencyBalanceFetcher(
        balanceFetchingOperations: BalanceFetchingOperations,
        appScope: AppCoroutineScope,
    ): CryptoCurrencyBalanceFetcher {
        return CryptoCurrencyBalanceFetcher(
            balanceFetchingOperations = balanceFetchingOperations,
            parallelUpdatingScope = appScope,
        )
    }

    @Provides
    @Singleton
    fun provideToggleTokenListSortingUseCaseV2(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListSortingUseCase {
        return ToggleTokenListSortingUseCase(
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideToggleTokenListGroupingUseCaseV2(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListGroupingUseCase {
        return ToggleTokenListGroupingUseCase(
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetFeePaidCryptoCurrencyStatusSyncUseCase(
        currenciesRepository: CurrenciesRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    ): GetFeePaidCryptoCurrencyStatusSyncUseCase {
        return GetFeePaidCryptoCurrencyStatusSyncUseCase(
            currenciesRepository = currenciesRepository,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideCryptoCurrencyMetadataCleaner(
        networksCleaner: NetworksCleaner,
        stakingCleaner: StakingCleaner,
        nftCleaner: NFTCleaner,
        dispatchers: CoroutineDispatcherProvider,
    ): CryptoCurrencyMetadataCleaner {
        return CryptoCurrencyMetadataCleaner(
            networksCleaner = networksCleaner,
            stakingCleaner = stakingCleaner,
            nftCleaner = nftCleaner,
            dispatchers = dispatchers,
        )
    }
}