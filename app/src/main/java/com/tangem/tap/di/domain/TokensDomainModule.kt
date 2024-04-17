package com.tangem.tap.di.domain

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.settings.ShouldShowSwapPromoTokenUseCase
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.repository.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions", "LargeClass")
internal object TokensDomainModule {

    @Provides
    @ViewModelScoped
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
    @ViewModelScoped
    fun provideFetchTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchTokenListUseCase {
        return FetchTokenListUseCase(currenciesRepository, networksRepository, quotesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideFetchPendingTransactionsUseCase(
        networksRepository: NetworksRepository,
    ): FetchPendingTransactionsUseCase {
        return FetchPendingTransactionsUseCase(networksRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): GetTokenListUseCase {
        return GetTokenListUseCase(currenciesRepository, quotesRepository, networksRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCardTokensListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): GetCardTokensListUseCase {
        return GetCardTokensListUseCase(currenciesRepository, quotesRepository, networksRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideRemoveCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        walletManagersFacade: WalletManagersFacade,
    ): RemoveCurrencyUseCase {
        return RemoveCurrencyUseCase(currenciesRepository, walletManagersFacade)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCurrencyStatusUpdatesUseCase {
        return GetCurrencyStatusUpdatesUseCase(currenciesRepository, quotesRepository, networksRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
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
            dispatchers = dispatchers,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetPrimaryCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetPrimaryCurrencyStatusUpdatesUseCase {
        return GetPrimaryCurrencyStatusUpdatesUseCase(
            currenciesRepository,
            quotesRepository,
            networksRepository,
            dispatchers,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideFetchCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchCurrencyStatusUseCase {
        return FetchCurrencyStatusUseCase(currenciesRepository, networksRepository, quotesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideFetchCardTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchCardTokenListUseCase {
        return FetchCardTokenListUseCase(currenciesRepository, networksRepository, quotesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCryptoCurrencyUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrencyUseCase {
        return GetCryptoCurrencyUseCase(currenciesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideToggleTokenListGroupingUseCase(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListGroupingUseCase {
        return ToggleTokenListGroupingUseCase(dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideToggleTokenListSortingUseCase(dispatchers: CoroutineDispatcherProvider): ToggleTokenListSortingUseCase {
        return ToggleTokenListSortingUseCase(dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideApplyTokenListSortingUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCase {
        return ApplyTokenListSortingUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCryptoCurrencyActionsUseCase(
        rampStateManager: RampStateManager,
        marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        sendFeatureToggles: SendFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCryptoCurrencyActionsUseCase {
        return GetCryptoCurrencyActionsUseCase(
            rampManager = rampStateManager,
            marketCryptoCurrencyRepository = marketCryptoCurrencyRepository,
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            sendFeatureToggles = sendFeatureToggles,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetCurrencyStatusByNetworkUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetNetworkCoinStatusUseCase {
        return GetNetworkCoinStatusUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetFeePaidCryptoCurrencyStatusSyncUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetFeePaidCryptoCurrencyStatusSyncUseCase {
        return GetFeePaidCryptoCurrencyStatusSyncUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetCurrenciesUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrenciesUseCase {
        return GetCryptoCurrenciesUseCase(currenciesRepository = currenciesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideIsCryptoCurrencyCoinCouldHideUseCase(
        currenciesRepository: CurrenciesRepository,
    ): IsCryptoCurrencyCoinCouldHideUseCase {
        return IsCryptoCurrencyCoinCouldHideUseCase(
            currenciesRepository = currenciesRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateDelayedCurrencyStatusUseCase(
        networksRepository: NetworksRepository,
    ): UpdateDelayedNetworkStatusUseCase {
        return UpdateDelayedNetworkStatusUseCase(
            networksRepository = networksRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideHasMissedAddressesCryptoCurrenciesUseCase(
        currenciesRepository: CurrenciesRepository,
    ): GetMissedAddressesCryptoCurrenciesUseCase {
        return GetMissedAddressesCryptoCurrenciesUseCase(currenciesRepository = currenciesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetGlobalTokenListUseCase(tokensListRepository: TokensListRepository): GetGlobalTokenListUseCase {
        return GetGlobalTokenListUseCase(repository = tokensListRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideCheckTokenCompatibilityUseCase(
        networksCompatibilityRepository: NetworksCompatibilityRepository,
    ): CheckCurrencyCompatibilityUseCase {
        return CheckCurrencyCompatibilityUseCase(networksCompatibilityRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideNeedHardenedDerivationUseCase(
        networksCompatibilityRepository: NetworksCompatibilityRepository,
    ): RequiresHardenedDerivationOnlyUseCase {
        return RequiresHardenedDerivationOnlyUseCase(networksCompatibilityRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideFindTokenByContractAddressUseCase(
        tokensListRepository: TokensListRepository,
    ): FindTokenByContractAddressUseCase {
        return FindTokenByContractAddressUseCase(repository = tokensListRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideValidateContractAddressUseCase(
        tokensListRepository: TokensListRepository,
    ): ValidateContractAddressUseCase {
        return ValidateContractAddressUseCase(tokensListRepository = tokensListRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideAreTokensSupportedByNetworkUseCase(
        repository: NetworksCompatibilityRepository,
    ): AreTokensSupportedByNetworkUseCase {
        return AreTokensSupportedByNetworkUseCase(repository = repository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetNetworksSupportedByWallet(
        repository: NetworksCompatibilityRepository,
    ): GetNetworksSupportedByWallet {
        return GetNetworksSupportedByWallet(repository = repository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetBalanceNotEnoughForFeeWarningUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetBalanceNotEnoughForFeeWarningUseCase {
        return GetBalanceNotEnoughForFeeWarningUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideIsAmountSubtractAvailableUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): IsAmountSubtractAvailableUseCase {
        return IsAmountSubtractAvailableUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideRunPolkadotAccountHealthCheckUseCase(
        repository: PolkadotAccountHealthCheckRepository,
    ): RunPolkadotAccountHealthCheckUseCase {
        return RunPolkadotAccountHealthCheckUseCase(repository)
    }
}