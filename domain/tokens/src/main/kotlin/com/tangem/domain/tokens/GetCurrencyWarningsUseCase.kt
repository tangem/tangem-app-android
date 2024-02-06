package com.tangem.domain.tokens

import com.tangem.domain.settings.ShouldShowSwapPromoTokenUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

@Suppress("LongParameterList")
class GetCurrencyWarningsUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val swapRepository: SwapRepository,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val showSwapPromoTokenUseCase: ShouldShowSwapPromoTokenUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Set<CryptoCurrencyWarning>> {
        val currency = currencyStatus.currency
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )
        return combine(
            getCoinRelatedWarnings(
                userWalletId = userWalletId,
                operations = operations,
                networkId = currency.network.id,
                currencyId = currency.id,
                derivationPath = derivationPath,
                isSingleWalletWithTokens = isSingleWalletWithTokens,
            ),
            flowOf(walletManagersFacade.getRentInfo(userWalletId, currency.network)),
            flowOf(currencyChecksRepository.getExistentialDeposit(userWalletId, currency.network)),
            getSwapPromoNotificationWarning(
                operations = operations,
                userWalletId = userWalletId,
                currencyStatus = currencyStatus,
            ).conflate(),
        ) { coinRelatedWarnings, maybeRentWarning, maybeEdWarning, maybeSwapPromo ->
            setOfNotNull(
                maybeSwapPromo,
                maybeRentWarning,
                maybeEdWarning?.let {
                    CryptoCurrencyWarning.ExistentialDeposit(
                        currencyName = currency.name,
                        edStringValueWithSymbol = "${it.toPlainString()} ${currency.symbol}",
                    )
                },
                *coinRelatedWarnings.toTypedArray(),
                getNetworkUnavailableWarning(currencyStatus),
                getNetworkNoAccountWarning(currencyStatus),
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getSwapPromoNotificationWarning(
        operations: CurrenciesStatusesOperations,
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
    ): Flow<CryptoCurrencyWarning?> {
        val currency = currencyStatus.currency
        val cryptoStatuses = operations.getCurrenciesStatusesSync()
        return combine(
            showSwapPromoTokenUseCase().conflate(),
            flowOf(marketCryptoCurrencyRepository.isExchangeable(userWalletId, currency)).conflate(),
        ) { shouldShowSwapPromo, isExchangeable ->
            if (shouldShowSwapPromo && isExchangeable && currencyStatus.value !is CryptoCurrencyStatus.Unreachable) {
                cryptoStatuses.fold(
                    ifLeft = { null },
                    ifRight = { cryptoCurrencyStatuses ->
                        val pairs = runCatching(dispatchers.io) {
                            swapRepository.getPairsOnly(
                                LeastTokenInfo(
                                    contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
                                    network = currency.network.backendId,
                                ),
                                cryptoCurrencyStatuses.map { it.currency },
                            )
                        }.getOrNull()?.pairs ?: emptyList()

                        val filteredCurrencies = cryptoCurrencyStatuses.filterNot {
                            it.currency.id == currency.id
                        }
                        val currencyPairs = pairs.filter {
                            it.from.network == currency.network.backendId ||
                                it.to.network == currency.network.backendId
                        }
                        val showPromo = currencyPairs.any { pair ->
                            val availablePair = if (currencyStatus.value.amount.isNullOrZero()) {
                                filteredCurrencies.filterNot { it.value.amount.isNullOrZero() }
                            } else {
                                filteredCurrencies
                            }
                            availablePair
                                .any {
                                    it.currency.network.backendId == pair.to.network ||
                                        it.currency.network.backendId == pair.from.network
                                }
                        }
                        if (showPromo) {
                            CryptoCurrencyWarning.SwapPromo
                        } else {
                            null
                        }
                    },
                )
            } else {
                null
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun getCoinRelatedWarnings(
        userWalletId: UserWalletId,
        operations: CurrenciesStatusesOperations,
        networkId: Network.ID,
        currencyId: CryptoCurrency.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<List<CryptoCurrencyWarning>> {
        val currencyFlow = if (isSingleWalletWithTokens) {
            operations.getCurrencyStatusSingleWalletWithTokensFlow(currencyId)
        } else {
            operations.getCurrencyStatusFlow(currencyId)
        }
        val networkFlow = if (isSingleWalletWithTokens) {
            operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
        } else {
            operations.getNetworkCoinFlow(networkId, derivationPath)
        }
        return combine(
            currencyFlow.map { it.getOrNull() },
            networkFlow.map { it.getOrNull() },
        ) { tokenStatus, coinStatus ->
            when {
                tokenStatus != null && coinStatus != null -> {
                    buildList {
                        if (currenciesRepository.hasPendingTransactions(tokenStatus, coinStatus)) {
                            add(CryptoCurrencyWarning.HasPendingTransactions(coinStatus.currency.symbol))
                        }
                        getFeeWarning(
                            userWalletId = userWalletId,
                            coinStatus = coinStatus,
                            tokenStatus = tokenStatus,
                        )?.let(::add)
                    }
                }
                else -> listOf(CryptoCurrencyWarning.SomeNetworksUnreachable)
            }
        }
    }

    private suspend fun getFeeWarning(
        userWalletId: UserWalletId,
        coinStatus: CryptoCurrencyStatus,
        tokenStatus: CryptoCurrencyStatus,
    ): CryptoCurrencyWarning? {
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, tokenStatus.currency)
        return when {
            feePaidCurrency is FeePaidCurrency.Coin &&
                !tokenStatus.value.amount.isZero() &&
                coinStatus.value.amount.isZero() -> {
                CryptoCurrencyWarning.BalanceNotEnoughForFee(
                    tokenCurrency = tokenStatus.currency,
                    coinCurrency = coinStatus.currency,
                )
            }
            feePaidCurrency is FeePaidCurrency.SameCurrency && !tokenStatus.value.amount.isZero() -> {
                CryptoCurrencyWarning.BalanceNotEnoughForFee(
                    tokenCurrency = tokenStatus.currency,
                    coinCurrency = coinStatus.currency,
                )
            }
            feePaidCurrency is FeePaidCurrency.Token -> {
                val feePaidTokenBalance = feePaidCurrency.balance
                val amount = tokenStatus.value.amount ?: return null
                if (!amount.isZero() && feePaidTokenBalance.isZero()) {
                    constructTokenBalanceNotEnoughWarning(
                        userWalletId = userWalletId,
                        tokenStatus = tokenStatus,
                        feePaidToken = feePaidCurrency,
                    )
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private suspend fun constructTokenBalanceNotEnoughWarning(
        userWalletId: UserWalletId,
        tokenStatus: CryptoCurrencyStatus,
        feePaidToken: FeePaidCurrency.Token,
    ): CryptoCurrencyWarning {
        val token = currenciesRepository
            .getMultiCurrencyWalletCurrenciesSync(userWalletId)
            .find {
                it is CryptoCurrency.Token &&
                    it.contractAddress.equals(feePaidToken.contractAddress, ignoreCase = true) &&
                    it.network.derivationPath == tokenStatus.currency.network.derivationPath
            }
        return if (token != null) {
            CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                currency = tokenStatus.currency,
                feeCurrency = token,
                networkName = token.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        } else {
            CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                currency = tokenStatus.currency,
                feeCurrency = null,
                networkName = tokenStatus.currency.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        }
    }

    private fun getNetworkUnavailableWarning(currencyStatus: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return (currencyStatus.value as? CryptoCurrencyStatus.Unreachable)?.let {
            CryptoCurrencyWarning.SomeNetworksUnreachable
        }
    }

    private fun getNetworkNoAccountWarning(currencyStatus: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return (currencyStatus.value as? CryptoCurrencyStatus.NoAccount)?.let {
            if (networksRepository.isNeedToCreateAccountWithoutReserve(network = currencyStatus.currency.network)) {
                CryptoCurrencyWarning.TopUpWithoutReserve
            } else {
                CryptoCurrencyWarning.SomeNetworksNoAccount(
                    amountToCreateAccount = it.amountToCreateAccount,
                    amountCurrency = currencyStatus.currency,
                )
            }
        }
    }

    private fun BigDecimal?.isZero(): Boolean {
        return this?.signum() == 0
    }
}
