package com.tangem.tap.di.domain

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.settings.ShouldShowSwapPromoTokenUseCase
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.repository.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
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
    ): AddCryptoCurrenciesUseCase {
        return AddCryptoCurrenciesUseCase(
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
        )
    }

    @Provides
    @Singleton
    fun provideFetchTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchTokenListUseCase {
        return FetchTokenListUseCase(currenciesRepository, networksRepository, quotesRepository)
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
    fun provideGetTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): GetTokenListUseCase {
        return GetTokenListUseCase(
            currenciesRepository,
            quotesRepository,
            networksRepository,
            stakingRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetCardTokensListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): GetCardTokensListUseCase {
        return GetCardTokensListUseCase(currenciesRepository, quotesRepository, networksRepository, stakingRepository)
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
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCurrencyStatusUpdatesUseCase {
        return GetCurrencyStatusUpdatesUseCase(
            currenciesRepository,
            quotesRepository,
            networksRepository,
            stakingRepository,
            dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrencyWarningsUseCase(
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
        swapRepository: SwapRepository,
        currencyChecksRepository: CurrencyChecksRepository,
        showSwapPromoTokenUseCase: ShouldShowSwapPromoTokenUseCase,
        promoRepository: PromoRepository,
        stakingRepository: StakingRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCurrencyWarningsUseCase {
        return GetCurrencyWarningsUseCase(
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            marketCryptoCurrencyRepository = marketCryptoCurrencyRepository,
            currencyChecksRepository = currencyChecksRepository,
            swapRepository = swapRepository,
            showSwapPromoTokenUseCase = showSwapPromoTokenUseCase,
            promoRepository = promoRepository,
            stakingRepository = stakingRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetPrimaryCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetPrimaryCurrencyStatusUpdatesUseCase {
        return GetPrimaryCurrencyStatusUpdatesUseCase(
            currenciesRepository,
            quotesRepository,
            networksRepository,
            stakingRepository,
            dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideFetchCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchCurrencyStatusUseCase {
        return FetchCurrencyStatusUseCase(currenciesRepository, networksRepository, quotesRepository)
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
        marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        stakingFeatureToggles: StakingFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCryptoCurrencyActionsUseCase {
        return GetCryptoCurrencyActionsUseCase(
            rampManager = rampStateManager,
            walletManagersFacade = walletManagersFacade,
            marketCryptoCurrencyRepository = marketCryptoCurrencyRepository,
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            stakingFeatureToggles = stakingFeatureToggles,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrencyStatusByNetworkUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetNetworkCoinStatusUseCase {
        return GetNetworkCoinStatusUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetFeePaidCryptoCurrencyStatusSyncUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetFeePaidCryptoCurrencyStatusSyncUseCase {
        return GetFeePaidCryptoCurrencyStatusSyncUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetCurrenciesUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrenciesUseCase {
        return GetCryptoCurrenciesUseCase(currenciesRepository = currenciesRepository)
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
    fun provideHasMissedAddressesCryptoCurrenciesUseCase(
        currenciesRepository: CurrenciesRepository,
    ): GetMissedAddressesCryptoCurrenciesUseCase {
        return GetMissedAddressesCryptoCurrenciesUseCase(currenciesRepository = currenciesRepository)
    }

    @Provides
    @Singleton
    fun provideGetGlobalTokenListUseCase(tokensListRepository: TokensListRepository): GetGlobalTokenListUseCase {
        return GetGlobalTokenListUseCase(repository = tokensListRepository)
    }

    @Provides
    @Singleton
    fun provideCheckTokenCompatibilityUseCase(
        networksCompatibilityRepository: NetworksCompatibilityRepository,
    ): CheckCurrencyCompatibilityUseCase {
        return CheckCurrencyCompatibilityUseCase(networksCompatibilityRepository)
    }

    @Provides
    @Singleton
    fun provideNeedHardenedDerivationUseCase(
        networksCompatibilityRepository: NetworksCompatibilityRepository,
    ): RequiresHardenedDerivationOnlyUseCase {
        return RequiresHardenedDerivationOnlyUseCase(networksCompatibilityRepository)
    }

    @Provides
    @Singleton
    fun provideFindTokenByContractAddressUseCase(
        tokensListRepository: TokensListRepository,
    ): FindTokenByContractAddressUseCase {
        return FindTokenByContractAddressUseCase(repository = tokensListRepository)
    }

    @Provides
    @Singleton
    fun provideValidateContractAddressUseCase(
        tokensListRepository: TokensListRepository,
    ): ValidateContractAddressUseCase {
        return ValidateContractAddressUseCase(tokensListRepository = tokensListRepository)
    }

    @Provides
    @Singleton
    fun provideAreTokensSupportedByNetworkUseCase(
        repository: NetworksCompatibilityRepository,
    ): AreTokensSupportedByNetworkUseCase {
        return AreTokensSupportedByNetworkUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideGetNetworksSupportedByWallet(
        repository: NetworksCompatibilityRepository,
    ): GetNetworksSupportedByWallet {
        return GetNetworksSupportedByWallet(repository = repository)
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
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
    ): GetWalletTotalBalanceUseCase {
        return GetWalletTotalBalanceUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
        )
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
}
