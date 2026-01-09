package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import com.tangem.domain.staking.single.SingleStakingBalanceSupplier
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CachedCurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.tokens.repository.TokenReceiveWarningsViewedRepository
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.tap.domain.tokens.DefaultTokensFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions", "LargeClass")
internal object TokensDomainModule {

    @Provides
    @Singleton
    fun provideAddCryptoCurrenciesUseCase(
        currenciesRepository: CurrenciesRepository,
        walletManagersFacade: WalletManagersFacade,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        singleStakingBalanceFetcher: SingleStakingBalanceFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        stakingIdFactory: StakingIdFactory,
    ): AddCryptoCurrenciesUseCase {
        return AddCryptoCurrenciesUseCase(
            currenciesRepository = currenciesRepository,
            walletManagersFacade = walletManagersFacade,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            singleStakingBalanceFetcher = singleStakingBalanceFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            stakingIdFactory = stakingIdFactory,
        )
    }

    @Provides
    @Singleton
    fun provideFetchPendingTransactionsUseCase(
        networksRepository: NetworksRepository,
    ): FetchPendingTransactionsUseCase {
        return FetchPendingTransactionsUseCase(networksRepository)
    }

    @Provides
    @Singleton
    fun provideTokensFeatureToggles(featureTogglesManager: FeatureTogglesManager): TokensFeatureToggles {
        return DefaultTokensFeatureToggles(featureTogglesManager = featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideGetTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        currenciesStatusesOperations: BaseCurrencyStatusOperations,
    ): GetTokenListUseCase {
        return GetTokenListUseCase(
            currenciesRepository = currenciesRepository,
            currenciesStatusesOperations = currenciesStatusesOperations,
        )
    }

    @Provides
    @Singleton
    fun provideRemoveCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        walletManagersFacade: WalletManagersFacade,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): RemoveCurrencyUseCase {
        return RemoveCurrencyUseCase(
            currenciesRepository = currenciesRepository,
            walletManagersFacade = walletManagersFacade,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrencyUseCase(
        baseCurrencyStatusOperations: BaseCurrencyStatusOperations,
        dispatchers: CoroutineDispatcherProvider,
    ): GetSingleCryptoCurrencyStatusUseCase {
        return GetSingleCryptoCurrencyStatusUseCase(
            currencyStatusOperations = baseCurrencyStatusOperations,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetAllWalletsCryptoCurrencyStatusesUseCase(
        currenciesRepository: CurrenciesRepository,
        currencyStatusOperations: BaseCurrencyStatusOperations,
        dispatchers: CoroutineDispatcherProvider,
    ): GetAllWalletsCryptoCurrencyStatusesUseCase {
        return GetAllWalletsCryptoCurrencyStatusesUseCase(
            currenciesRepository = currenciesRepository,
            currencyStatusOperations = currencyStatusOperations,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrencyWarningsUseCase(
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        currencyChecksRepository: CurrencyChecksRepository,
        dispatchers: CoroutineDispatcherProvider,
        baseCurrencyStatusOperations: BaseCurrencyStatusOperations,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): GetCurrencyWarningsUseCase {
        return GetCurrencyWarningsUseCase(
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            dispatchers = dispatchers,
            currencyChecksRepository = currencyChecksRepository,
            currencyStatusOperations = baseCurrencyStatusOperations,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideFetchCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        singleStakingBalanceFetcher: SingleStakingBalanceFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        stakingIdFactory: StakingIdFactory,
    ): FetchCurrencyStatusUseCase {
        return FetchCurrencyStatusUseCase(
            currenciesRepository = currenciesRepository,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            singleStakingBalanceFetcher = singleStakingBalanceFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            stakingIdFactory = stakingIdFactory,
        )
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): GetCryptoCurrencyUseCase {
        return GetCryptoCurrencyUseCase(currenciesRepository, multiWalletCryptoCurrenciesSupplier)
    }

    @Provides
    @Singleton
    fun provideToggleTokenListGroupingUseCase(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListGroupingUseCase {
        return ToggleTokenListGroupingUseCase(dispatchers)
    }

    @Provides
    @Singleton
    fun provideToggleTokenListSortingUseCase(dispatchers: CoroutineDispatcherProvider): ToggleTokenListSortingUseCase {
        return ToggleTokenListSortingUseCase(dispatchers)
    }

    @Provides
    @Singleton
    fun provideApplyTokenListSortingUseCase(
        currenciesRepository: CurrenciesRepository,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCase {
        return ApplyTokenListSortingUseCase(
            currenciesRepository = currenciesRepository,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrencyActionsUseCase(
        rampStateManager: RampStateManager,
        walletManagersFacade: WalletManagersFacade,
        stakingRepository: StakingRepository,
        promoRepository: PromoRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCryptoCurrencyActionsUseCase {
        return GetCryptoCurrencyActionsUseCase(
            rampManager = rampStateManager,
            walletManagersFacade = walletManagersFacade,
            stakingRepository = stakingRepository,
            promoRepository = promoRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrencyStatusByNetworkUseCase(
        currencyStatusOperations: BaseCurrencyStatusOperations,
        dispatchers: CoroutineDispatcherProvider,
    ): GetNetworkCoinStatusUseCase {
        return GetNetworkCoinStatusUseCase(
            currencyStatusOperations = currencyStatusOperations,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetFeePaidCryptoCurrencyStatusSyncUseCase(
        currenciesRepository: CurrenciesRepository,
        currencyStatusOperations: BaseCurrencyStatusOperations,
    ): GetFeePaidCryptoCurrencyStatusSyncUseCase {
        return GetFeePaidCryptoCurrencyStatusSyncUseCase(
            currenciesRepository = currenciesRepository,
            currencyStatusOperations = currencyStatusOperations,
        )
    }

    @Provides
    @Singleton
    fun provideGetMinimumTransactionAmountSyncUseCase(
        currencyChecksRepository: CurrencyChecksRepository,
    ): GetMinimumTransactionAmountSyncUseCase {
        return GetMinimumTransactionAmountSyncUseCase(
            currencyChecksRepository = currencyChecksRepository,
        )
    }

    @Provides
    @Singleton
    fun provideIsCryptoCurrencyCoinCouldHideUseCase(
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): IsCryptoCurrencyCoinCouldHideUseCase {
        return IsCryptoCurrencyCoinCouldHideUseCase(
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideUpdateDelayedCurrencyStatusUseCase(
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    ): UpdateDelayedNetworkStatusUseCase {
        return UpdateDelayedNetworkStatusUseCase(
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        )
    }

    @Provides
    @Singleton
    fun provideGetBalanceNotEnoughForFeeWarningUseCase(
        currenciesRepository: CurrenciesRepository,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        dispatchers: CoroutineDispatcherProvider,
    ): GetBalanceNotEnoughForFeeWarningUseCase {
        return GetBalanceNotEnoughForFeeWarningUseCase(
            currenciesRepository = currenciesRepository,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideIsAmountSubtractAvailableUseCase(
        currenciesRepository: CurrenciesRepository,
    ): IsAmountSubtractAvailableUseCase {
        return IsAmountSubtractAvailableUseCase(currenciesRepository)
    }

    @Provides
    @Singleton
    fun provideGetNetworkStatusesUseCase(networksRepository: NetworksRepository): GetNetworkAddressesUseCase {
        return GetNetworkAddressesUseCase(
            networksRepository = networksRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetWalletTotalBalanceUseCase(
        currenciesStatusesOperations: BaseCurrencyStatusOperations,
    ): GetWalletTotalBalanceUseCase {
        return GetWalletTotalBalanceUseCase(currenciesStatusesOperations)
    }

    @Provides
    @Singleton
    fun provideRefreshMultiCurrencyWalletQuotesUseCase(
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): RefreshMultiCurrencyWalletQuotesUseCase {
        return RefreshMultiCurrencyWalletQuotesUseCase(
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrencyCheckUseCase(
        currencyChecksRepository: CurrencyChecksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCurrencyCheckUseCase {
        return GetCurrencyCheckUseCase(currencyChecksRepository, dispatchers)
    }

    @Provides
    @Singleton
    fun provideBaseCurrencyStatusOperations(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
        singleStakingBalanceSupplier: SingleStakingBalanceSupplier,
        multiStakingBalanceSupplier: MultiStakingBalanceSupplier,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        stakingIdFactory: StakingIdFactory,
    ): BaseCurrencyStatusOperations {
        return CachedCurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            singleQuoteStatusSupplier = singleQuoteStatusSupplier,
            singleStakingBalanceSupplier = singleStakingBalanceSupplier,
            multiStakingBalanceSupplier = multiStakingBalanceSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            stakingIdFactory = stakingIdFactory,
        )
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrenciesUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrenciesUseCase {
        return GetCryptoCurrenciesUseCase(currenciesRepository)
    }

    @Provides
    @Singleton
    fun provideWalletBalanceFetcher(
        currenciesRepository: CurrenciesRepository,
        multiWalletCryptoCurrenciesFetcher: MultiWalletCryptoCurrenciesFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletBalanceFetcher {
        return WalletBalanceFetcher(
            currenciesRepository = currenciesRepository,
            multiWalletCryptoCurrenciesFetcher = multiWalletCryptoCurrenciesFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetAssetRequirementsUseCase(walletManagersFacade: WalletManagersFacade): GetAssetRequirementsUseCase {
        return GetAssetRequirementsUseCase(walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideGetViewedTokenReceiveWarningUseCase(
        tokenReceiveWarningsViewedRepository: TokenReceiveWarningsViewedRepository,
    ): GetViewedTokenReceiveWarningUseCase {
        return GetViewedTokenReceiveWarningUseCase(tokenReceiveWarningsViewedRepository)
    }

    @Provides
    @Singleton
    fun provideSaveViewedTokenReceiveWarningUseCase(
        tokenReceiveWarningsViewedRepository: TokenReceiveWarningsViewedRepository,
    ): SaveViewedTokenReceiveWarningUseCase {
        return SaveViewedTokenReceiveWarningUseCase(tokenReceiveWarningsViewedRepository)
    }

    @Provides
    @Singleton
    fun provideNeedShowYieldSupplyDepositedWarningUseCase(
        yieldSupplyWarningsViewedRepository: YieldSupplyWarningsViewedRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): NeedShowYieldSupplyDepositedWarningUseCase {
        return NeedShowYieldSupplyDepositedWarningUseCase(yieldSupplyWarningsViewedRepository, dispatchers)
    }

    @Provides
    @Singleton
    fun provideSaveViewedYieldSupplyWarningUseCase(
        yieldSupplyWarningsViewedRepository: YieldSupplyWarningsViewedRepository,
    ): SaveViewedYieldSupplyWarningUseCase {
        return SaveViewedYieldSupplyWarningUseCase(yieldSupplyWarningsViewedRepository)
    }
}