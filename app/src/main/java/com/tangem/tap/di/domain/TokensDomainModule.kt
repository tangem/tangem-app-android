package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.quotes.QuotesRepositoryV2
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.operations.BaseCurrenciesStatusesOperations
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.operations.CachedCurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.*
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
        stakingRepository: StakingRepository,
        quotesRepository: QuotesRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        singleYieldBalanceFetcher: SingleYieldBalanceFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): AddCryptoCurrenciesUseCase {
        return AddCryptoCurrenciesUseCase(
            currenciesRepository = currenciesRepository,
            stakingRepository = stakingRepository,
            quotesRepository = quotesRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            singleYieldBalanceFetcher = singleYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideFetchTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        stakingRepository: StakingRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): FetchTokenListUseCase {
        return FetchTokenListUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            stakingRepository = stakingRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
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
        baseCurrenciesStatusesOperations: BaseCurrenciesStatusesOperations,
    ): GetTokenListUseCase {
        return GetTokenListUseCase(
            currenciesRepository = currenciesRepository,
            currenciesStatusesOperations = baseCurrenciesStatusesOperations,
        )
    }

    @Provides
    @Singleton
    fun provideRemoveCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        walletManagersFacade: WalletManagersFacade,
    ): RemoveCurrencyUseCase {
        return RemoveCurrencyUseCase(currenciesRepository, walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideGetCurrencyUseCase(
        baseCurrencyStatusOperations: BaseCurrencyStatusOperations,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCurrencyStatusUpdatesUseCase {
        return GetCurrencyStatusUpdatesUseCase(
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
        networksRepository: NetworksRepository,
        currencyChecksRepository: CurrencyChecksRepository,
        dispatchers: CoroutineDispatcherProvider,
        baseCurrencyStatusOperations: BaseCurrencyStatusOperations,
    ): GetCurrencyWarningsUseCase {
        return GetCurrencyWarningsUseCase(
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
            currencyChecksRepository = currencyChecksRepository,
            dispatchers = dispatchers,
            currencyStatusOperations = baseCurrencyStatusOperations,
        )
    }

    @Provides
    @Singleton
    fun provideGetPrimaryCurrencyUseCase(
        currencyStatusOperations: BaseCurrencyStatusOperations,
        dispatchers: CoroutineDispatcherProvider,
    ): GetPrimaryCurrencyStatusUpdatesUseCase {
        return GetPrimaryCurrencyStatusUpdatesUseCase(
            currencyStatusOperations = currencyStatusOperations,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideFetchCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        stakingRepository: StakingRepository,
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        singleYieldBalanceFetcher: SingleYieldBalanceFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): FetchCurrencyStatusUseCase {
        return FetchCurrencyStatusUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            stakingRepository = stakingRepository,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            singleYieldBalanceFetcher = singleYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideFetchCardTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        stakingRepository: StakingRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): FetchCardTokenListUseCase {
        return FetchCardTokenListUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            stakingRepository = stakingRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun providesGetCryptoCurrencyStatusSyncUseCase(
        currencyStatusOperations: BaseCurrencyStatusOperations,
    ): GetCryptoCurrencyStatusSyncUseCase {
        return GetCryptoCurrencyStatusSyncUseCase(currencyStatusOperations)
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrencyUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrencyUseCase {
        return GetCryptoCurrencyUseCase(currenciesRepository)
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
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCase {
        return ApplyTokenListSortingUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrencyActionsUseCase(
        rampStateManager: RampStateManager,
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        stakingRepository: StakingRepository,
        promoRepository: PromoRepository,
        dispatchers: CoroutineDispatcherProvider,
        currencyStatusOperations: BaseCurrencyStatusOperations,
    ): GetCryptoCurrencyActionsUseCase {
        return GetCryptoCurrencyActionsUseCase(
            rampManager = rampStateManager,
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            stakingRepository = stakingRepository,
            promoRepository = promoRepository,
            dispatchers = dispatchers,
            currencyStatusOperations = currencyStatusOperations,
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
        currenciesRepository: CurrenciesRepository,
    ): IsCryptoCurrencyCoinCouldHideUseCase {
        return IsCryptoCurrencyCoinCouldHideUseCase(
            currenciesRepository = currenciesRepository,
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
        dispatchers: CoroutineDispatcherProvider,
    ): GetBalanceNotEnoughForFeeWarningUseCase {
        return GetBalanceNotEnoughForFeeWarningUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @Singleton
    fun provideIsAmountSubtractAvailableUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): IsAmountSubtractAvailableUseCase {
        return IsAmountSubtractAvailableUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @Singleton
    fun provideRunPolkadotAccountHealthCheckUseCase(
        repository: PolkadotAccountHealthCheckRepository,
    ): RunPolkadotAccountHealthCheckUseCase {
        return RunPolkadotAccountHealthCheckUseCase(repository)
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
        baseCurrenciesStatusesOperations: BaseCurrenciesStatusesOperations,
    ): GetWalletTotalBalanceUseCase {
        return GetWalletTotalBalanceUseCase(baseCurrenciesStatusesOperations)
    }

    @Provides
    @Singleton
    fun provideRefreshMultiCurrencyWalletQuotesUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        multiQuoteFetcher: MultiQuoteFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): RefreshMultiCurrencyWalletQuotesUseCase {
        return RefreshMultiCurrencyWalletQuotesUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            multiQuoteFetcher = multiQuoteFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
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
    fun provideBaseCurrenciesStatusesOperations(
        tokensFeatureToggles: TokensFeatureToggles,
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        quotesRepositoryV2: QuotesRepositoryV2,
        stakingRepository: StakingRepository,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        singleQuoteSupplier: SingleQuoteSupplier,
        singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    ): BaseCurrenciesStatusesOperations {
        return CachedCurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            quotesRepositoryV2 = quotesRepositoryV2,
            stakingRepository = stakingRepository,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            singleQuoteSupplier = singleQuoteSupplier,
            singleYieldBalanceSupplier = singleYieldBalanceSupplier,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideBaseCurrencyStatusOperations(
        tokensFeatureToggles: TokensFeatureToggles,
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        quotesRepositoryV2: QuotesRepositoryV2,
        stakingRepository: StakingRepository,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        singleQuoteSupplier: SingleQuoteSupplier,
        singleYieldBalanceSupplier: SingleYieldBalanceSupplier,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    ): BaseCurrencyStatusOperations {
        return CachedCurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            quotesRepositoryV2 = quotesRepositoryV2,
            stakingRepository = stakingRepository,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            singleQuoteSupplier = singleQuoteSupplier,
            singleYieldBalanceSupplier = singleYieldBalanceSupplier,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideGetCryptoCurrenciesUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrenciesUseCase {
        return GetCryptoCurrenciesUseCase(currenciesRepository)
    }
}