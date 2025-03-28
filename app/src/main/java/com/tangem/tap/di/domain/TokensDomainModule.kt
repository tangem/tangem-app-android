package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.staking.repositories.StakingRepository
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
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        quotesRepository: QuotesRepository,
    ): AddCryptoCurrenciesUseCase {
        return AddCryptoCurrenciesUseCase(
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            quotesRepository = quotesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideFetchTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): FetchTokenListUseCase {
        return FetchTokenListUseCase(currenciesRepository, networksRepository, quotesRepository, stakingRepository)
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
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): FetchCurrencyStatusUseCase {
        return FetchCurrencyStatusUseCase(currenciesRepository, networksRepository, quotesRepository, stakingRepository)
    }

    @Provides
    @Singleton
    fun provideFetchCardTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): FetchCardTokenListUseCase {
        return FetchCardTokenListUseCase(currenciesRepository, networksRepository, quotesRepository, stakingRepository)
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
        networksRepository: NetworksRepository,
    ): UpdateDelayedNetworkStatusUseCase {
        return UpdateDelayedNetworkStatusUseCase(
            networksRepository = networksRepository,
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
    ): RefreshMultiCurrencyWalletQuotesUseCase {
        return RefreshMultiCurrencyWalletQuotesUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
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
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): BaseCurrenciesStatusesOperations {
        return CachedCurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideBaseCurrencyStatusOperations(
        tokensFeatureToggles: TokensFeatureToggles,
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): BaseCurrencyStatusOperations {
        return CachedCurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }
}
