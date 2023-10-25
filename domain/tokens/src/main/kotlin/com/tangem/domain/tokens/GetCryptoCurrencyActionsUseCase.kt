package com.tangem.domain.tokens

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

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
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isSingleWalletWithTokens: Boolean,
    ): Flow<TokenActionsState> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            userWalletId = userWalletId,
        )
        val networkId = cryptoCurrencyStatus.currency.network.id
        val networkFlow = if (isSingleWalletWithTokens) {
            operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
        } else {
            operations.getNetworkCoinFlow(networkId, cryptoCurrencyStatus.currency.network.derivationPath)
        }
        return networkFlow.mapLatest { maybeCoinStatus ->
            createTokenActionsState(
                userWalletId = userWalletId,
                coinStatus = maybeCoinStatus.getOrNull(),
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWalletId: UserWalletId,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(userWalletId, coinStatus, cryptoCurrencyStatus),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Buy Send Receive Sell Swap]
     */
    private suspend fun createListOfActions(
        userWalletId: UserWalletId,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(true))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(cryptoCurrency)
        }

        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        activeList.add(TokenActionsState.ActionState.CopyAddress(true))

        // buy
        if (rampManager.availableForBuy(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Buy(false))
        }

        // send
        if (cryptoCurrencyStatus.value.amount.isNullOrZero() ||
            coinStatus?.value?.amount.isNullOrZero() ||
            coinStatus?.value?.hasCurrentNetworkTransactions == true
        ) {
            disabledList.add(TokenActionsState.ActionState.Send(false))
        } else {
            activeList.add(TokenActionsState.ActionState.Send(true))
        }

        // receive
        activeList.add(TokenActionsState.ActionState.Receive(true))

        // swap
        if (marketCryptoCurrencyRepository.isExchangeable(userWalletId, cryptoCurrency.id)) {
            activeList.add(TokenActionsState.ActionState.Swap(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Swap(false))
        }

        // sell
        if (rampManager.availableForSell(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Sell(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Sell(false))
        }

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(true))

        return activeList + disabledList
    }

    private fun getActionsForUnreachableCurrency(cryptoCurrency: CryptoCurrency): List<TokenActionsState.ActionState> {
        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        activeList.add(TokenActionsState.ActionState.CopyAddress(true))
        if (rampManager.availableForBuy(cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(true))
        } else {
            disabledList.add(TokenActionsState.ActionState.Buy(false))
        }
        disabledList.add(TokenActionsState.ActionState.Send(false))
        disabledList.add(TokenActionsState.ActionState.Swap(false))
        disabledList.add(TokenActionsState.ActionState.Sell(false))
        activeList.add(TokenActionsState.ActionState.Receive(true))
        activeList.add(TokenActionsState.ActionState.HideToken(true))
        return activeList + disabledList
    }
}