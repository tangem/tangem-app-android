package com.tangem.domain.tokens

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        resultTarget: ResultTarget = ResultTarget.MULTICURRENCY_WALET_MENU_ITEMS, // TODO
    ): Flow<TokenActionsState> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWallet.walletId,
        )
        val networkId = cryptoCurrencyStatus.currency.network.id

        return flow {
            val networkFlow = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
            } else if (!userWallet.isMultiCurrency) {
                operations.getPrimaryCurrencyStatusFlow()
            } else {
                operations.getNetworkCoinFlow(networkId, cryptoCurrencyStatus.currency.network.derivationPath)
            }

            val flow = networkFlow.mapLatest { maybeCoinStatus ->
                createTokenActionsState(
                    userWallet = userWallet,
                    coinStatus = maybeCoinStatus.getOrNull(),
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    resultTarget = resultTarget,
                )
            }

            emitAll(flow)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        resultTarget: ResultTarget,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(
                userWallet = userWallet,
                coinStatus = coinStatus,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                resultTarget = resultTarget,
            ),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Buy Send Receive Sell Swap]
     */
    private suspend fun createListOfActions(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        resultTarget: ResultTarget,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(cryptoCurrencyStatus)
        }

        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None))
        }

        // send
        val sendUnavailabilityReason = getSendUnavailabilityReason(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )
        actionsList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))

        // swap
        if (userWallet.isMultiCurrency) {
            if (marketCryptoCurrencyRepository.isExchangeable(userWallet.walletId, cryptoCurrency)) {
                actionsList.add(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None))
            } else {
                actionsList.add(
                    TokenActionsState.ActionState.Swap(
                        unavailabilityReason = ScenarioUnavailabilityReason.SellUnavailable(cryptoCurrency.name),
                    ),
                )
            }
        }

        // buy
        if (rampManager.availableForBuy(cryptoCurrency)) {
            actionsList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            actionsList.add(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrency.symbol)),
            )
        }

        // sell
        if (rampManager.availableForSell(cryptoCurrency)) {
            actionsList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None))
        } else {
            actionsList.add(
                TokenActionsState.ActionState.Sell(
                    unavailabilityReason = ScenarioUnavailabilityReason.SellUnavailable(cryptoCurrency.name),
                ),
            )
        }

        // hide
        actionsList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return actionsList
    }

    private fun getActionsForUnreachableCurrency(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TokenActionsState.ActionState> {
        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }
        if (rampManager.availableForBuy(cryptoCurrencyStatus.currency)) {
            actionsList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            actionsList.add(
                TokenActionsState.ActionState.Buy(
                    ScenarioUnavailabilityReason.BuyUnavailable(
                        cryptoCurrencyName = cryptoCurrencyStatus.currency.name,
                    ),
                ),
            )
        }
        actionsList.add(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.NoQuotes))
        actionsList.add(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.NoQuotes))
        actionsList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.NoQuotes))

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None))
        }
        actionsList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        return actionsList
    }

    private suspend fun getSendUnavailabilityReason(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): ScenarioUnavailabilityReason {
        val insufficientFundsForFee = insufficientFundsForFee(
            userWalletId = userWalletId,
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )

        return when {
            cryptoCurrencyStatus.value.amount.isNullOrZero() -> {
                ScenarioUnavailabilityReason.EmptyBalance
            }
            insufficientFundsForFee != null -> {
                ScenarioUnavailabilityReason.InsufficientFundsForFee(
                    currency = insufficientFundsForFee.currency,
                    networkName = insufficientFundsForFee.networkName,
                    feeCurrencyName = insufficientFundsForFee.feeCurrencyName,
                    feeCurrencySymbol = insufficientFundsForFee.feeCurrencySymbol,
                )
            }
            currenciesRepository.hasPendingTransactions(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                coinStatus = coinStatus,
            ) -> {
                ScenarioUnavailabilityReason.PendingTransaction(coinStatus?.currency?.symbol ?: "")
            }
            else -> {
                ScenarioUnavailabilityReason.None
            }
        }
    }

    private suspend fun insufficientFundsForFee(
        userWalletId: UserWalletId,
        tokenStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): FeeInfo? {
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, tokenStatus.currency)
        coinStatus ?: return null
        return when {
            feePaidCurrency is FeePaidCurrency.Coin &&
                !tokenStatus.value.amount.isZero() &&
                coinStatus.value.amount.isZero() -> {
                FeeInfo(
                    currency = tokenStatus.currency,
                    networkName = coinStatus.currency.network.name,
                    feeCurrencyName = coinStatus.currency.name,
                    feeCurrencySymbol = coinStatus.currency.symbol,
                )
            }
            feePaidCurrency is FeePaidCurrency.SameCurrency && !tokenStatus.value.amount.isZero() -> {
                FeeInfo(
                    currency = tokenStatus.currency,
                    networkName = coinStatus.currency.network.name,
                    feeCurrencyName = coinStatus.currency.name,
                    feeCurrencySymbol = coinStatus.currency.symbol,
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
    ): FeeInfo {
        val token = currenciesRepository
            .getMultiCurrencyWalletCurrenciesSync(userWalletId)
            .find {
                it is CryptoCurrency.Token &&
                    it.contractAddress.equals(feePaidToken.contractAddress, ignoreCase = true) &&
                    it.network.derivationPath == tokenStatus.currency.network.derivationPath
            }
        return if (token != null) {
            FeeInfo(
                currency = tokenStatus.currency,
                networkName = token.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        } else {
            FeeInfo(
                currency = tokenStatus.currency,
                networkName = tokenStatus.currency.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        }
    }

    private fun isAddressAvailable(networkAddress: NetworkAddress?): Boolean {
        return networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()
    }

    private fun BigDecimal?.isZero(): Boolean {
        return this?.signum() == 0
    }

    enum class ResultTarget {
        MULTICURRENCY_WALET_MENU_ITEMS,
        TOKEN_BUTTONS,
    }

    data class FeeInfo(
        val currency: CryptoCurrency,
        val networkName: String,
        val feeCurrencyName: String,
        val feeCurrencySymbol: String,
    )
}
