package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.tokens.CardCryptoCurrencyFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.account.status.usecase.IsCryptoCurrencyCouldHideUseCase
import com.tangem.domain.tokens.*
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
internal object TokensDomainModule {

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
    fun provideGetMinimumTransactionAmountSyncUseCase(
        currencyChecksRepository: CurrencyChecksRepository,
    ): GetMinimumTransactionAmountSyncUseCase {
        return GetMinimumTransactionAmountSyncUseCase(
            currencyChecksRepository = currencyChecksRepository,
        )
    }

    @Provides
    @Singleton
    fun provideIsCryptoCurrencyCouldHideUseCase(
        singleAccountListSupplier: SingleAccountListSupplier,
    ): IsCryptoCurrencyCouldHideUseCase {
        return IsCryptoCurrencyCouldHideUseCase(
            singleAccountListSupplier = singleAccountListSupplier,
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
        currencyChecksRepository: CurrencyChecksRepository,
    ): GetBalanceNotEnoughForFeeWarningUseCase {
        return GetBalanceNotEnoughForFeeWarningUseCase(
            currenciesRepository = currenciesRepository,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            dispatchers = dispatchers,
            currencyChecksRepository = currencyChecksRepository,
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
    fun provideWalletBalanceFetcher(
        userWalletsListRepository: UserWalletsListRepository,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        expressServiceFetcher: ExpressServiceFetcher,
        multiWalletAccountListFetcher: MultiWalletAccountListFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
        paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        stakingIdFactory: StakingIdFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletBalanceFetcher {
        return WalletBalanceFetcher(
            userWalletsListRepository = userWalletsListRepository,
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
            expressServiceFetcher = expressServiceFetcher,
            multiWalletAccountListFetcher = multiWalletAccountListFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            paymentAccountStatusFetcher = paymentAccountStatusFetcher,
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