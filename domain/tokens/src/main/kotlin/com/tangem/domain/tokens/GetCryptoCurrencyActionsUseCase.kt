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
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
@Suppress("LongParameterList")
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWallet: UserWallet, cryptoCurrencyStatus: CryptoCurrencyStatus): Flow<TokenActionsState> {
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
                )
            }

            emitAll(flow)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(
                userWallet = userWallet,
                coinStatus = coinStatus,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Receive Send Swap Buy Sell]
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun createListOfActions(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(cryptoCurrencyStatus)
        }

        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None))
        }

        // send
        val sendUnavailabilityReason = getSendUnavailabilityReason(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )
        if (sendUnavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))
        } else {
            disabledList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))
        }

        // swap
        if (userWallet.isMultiCurrency) {
            if (
                marketCryptoCurrencyRepository.isExchangeable(userWallet.walletId, cryptoCurrency) &&
                cryptoCurrencyStatus.value !is CryptoCurrencyStatus.NoQuote
            ) {
                activeList.add(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None))
            } else {
                disabledList.add(
                    TokenActionsState.ActionState.Swap(
                        unavailabilityReason = ScenarioUnavailabilityReason.NotExchangeable(cryptoCurrency.name),
                    ),
                )
            }
        }

        // buy
        if (rampManager.availableForBuy(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            disabledList.add(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrency.symbol)),
            )
        }

        // sell
        val sellSupportedByService = rampManager.availableForSell(cryptoCurrency)
        val sendAvailable = sendUnavailabilityReason is ScenarioUnavailabilityReason.None

        when {
            sellSupportedByService && sendAvailable -> {
                activeList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None))
            }
            sellSupportedByService && !sendAvailable -> {
                (sendUnavailabilityReason as? ScenarioUnavailabilityReason.EmptyBalance)?.let {
                    disabledList.add(
                        TokenActionsState.ActionState.Sell(
                            unavailabilityReason = it.copy(
                                withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL,
                            ),
                        ),
                    )
                }
                (sendUnavailabilityReason as? ScenarioUnavailabilityReason.PendingTransaction)?.let {
                    disabledList.add(
                        TokenActionsState.ActionState.Sell(
                            unavailabilityReason = it.copy(
                                withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL,
                            ),
                        ),
                    )
                }
            }
            else -> {
                disabledList.add(
                    TokenActionsState.ActionState.Sell(
                        unavailabilityReason = ScenarioUnavailabilityReason.NotSupportedBySellService(
                            cryptoCurrency.name,
                        ),
                    ),
                )
            }
        }

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList + disabledList
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
        actionsList.add(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.Unreachable))
        actionsList.add(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.Unreachable))
        actionsList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.Unreachable))

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None))
        }
        actionsList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        return actionsList
    }

    private fun getSendUnavailabilityReason(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): ScenarioUnavailabilityReason {
        return when {
            cryptoCurrencyStatus.value.amount.isNullOrZero() -> {
                ScenarioUnavailabilityReason.EmptyBalance(ScenarioUnavailabilityReason.WithdrawalScenario.SEND)
            }
            currenciesRepository.hasPendingTransactions(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                coinStatus = coinStatus,
            ) -> {
                ScenarioUnavailabilityReason.PendingTransaction(
                    withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SEND,
                    cryptoCurrencySymbol = coinStatus?.currency?.symbol.orEmpty(),
                )
            }
            else -> {
                ScenarioUnavailabilityReason.None
            }
        }
    }

    private fun isAddressAvailable(networkAddress: NetworkAddress?): Boolean {
        return networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()
    }
}
