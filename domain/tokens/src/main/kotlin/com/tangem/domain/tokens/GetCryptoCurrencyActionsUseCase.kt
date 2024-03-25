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
            return listOf(TokenActionsState.ActionState.HideToken(ButtonDisabledReason.NONE))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(cryptoCurrencyStatus)
        }

        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ButtonDisabledReason.NONE))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.Receive(ButtonDisabledReason.NONE))
        }

        // send
        val sendDisabledReason = getSendDisabledReason(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )
        actionsList.add(TokenActionsState.ActionState.Send(sendDisabledReason))

        // swap
        if (userWallet.isMultiCurrency) {
            if (marketCryptoCurrencyRepository.isExchangeable(userWallet.walletId, cryptoCurrency)) {
                actionsList.add(TokenActionsState.ActionState.Swap(ButtonDisabledReason.NONE))
            } else {
                actionsList.add(TokenActionsState.ActionState.Swap(ButtonDisabledReason.SELL_UNAVAILABLE))
            }
        }

        // buy
        if (rampManager.availableForBuy(cryptoCurrency)) {
            actionsList.add(TokenActionsState.ActionState.Buy(ButtonDisabledReason.NONE))
        } else {
            actionsList.add(TokenActionsState.ActionState.Buy(ButtonDisabledReason.BUY_UNAVAILABLE))
        }

        // sell
        if (rampManager.availableForSell(cryptoCurrency)) {
            actionsList.add(TokenActionsState.ActionState.Sell(ButtonDisabledReason.NONE))
        } else {
            actionsList.add(TokenActionsState.ActionState.Sell(ButtonDisabledReason.SELL_UNAVAILABLE))
        }

        // hide
        actionsList.add(TokenActionsState.ActionState.HideToken(ButtonDisabledReason.NONE))

        return actionsList
    }

    private fun getActionsForUnreachableCurrency(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TokenActionsState.ActionState> {
        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ButtonDisabledReason.NONE))
        }
        if (rampManager.availableForBuy(cryptoCurrencyStatus.currency)) {
            actionsList.add(TokenActionsState.ActionState.Buy(ButtonDisabledReason.NONE))
        } else {
            actionsList.add(TokenActionsState.ActionState.Buy(ButtonDisabledReason.BUY_UNAVAILABLE))
        }
        actionsList.add(TokenActionsState.ActionState.Send(ButtonDisabledReason.GENERAL_ERROR))
        actionsList.add(TokenActionsState.ActionState.Swap(ButtonDisabledReason.GENERAL_ERROR))
        actionsList.add(TokenActionsState.ActionState.Sell(ButtonDisabledReason.GENERAL_ERROR))

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.Receive(ButtonDisabledReason.NONE))
        }
        actionsList.add(TokenActionsState.ActionState.HideToken(ButtonDisabledReason.NONE))
        return actionsList
    }

    private suspend fun getSendDisabledReason(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): ButtonDisabledReason {
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, cryptoCurrencyStatus.currency)
        val insufficientFundsForFee = insufficientFundsForFee(
            feePaidCurrency = feePaidCurrency,
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )

        return when {
            cryptoCurrencyStatus.value.amount.isNullOrZero() -> {
                ButtonDisabledReason.EMPTY_BALANCE
            }
            insufficientFundsForFee -> {
                ButtonDisabledReason.INSUFFICIENT_FUNDS_FOR_FEE
            }
            currenciesRepository.hasPendingTransactions(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                coinStatus = coinStatus,
            ) -> {
                ButtonDisabledReason.PENDING_TRANSACTION
            }
            else -> {
                ButtonDisabledReason.NONE
            }
        }
    }

    private fun insufficientFundsForFee(
        feePaidCurrency: FeePaidCurrency,
        tokenStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): Boolean {
        return when (feePaidCurrency) {
            FeePaidCurrency.Coin -> !tokenStatus.value.amount.isZero() && coinStatus?.value?.amount.isZero()
            FeePaidCurrency.SameCurrency -> tokenStatus.value.amount.isZero()
            is FeePaidCurrency.Token -> {
                val feePaidTokenBalance = feePaidCurrency.balance
                !tokenStatus.value.amount.isZero() && feePaidTokenBalance.isZero()
            }
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
}
