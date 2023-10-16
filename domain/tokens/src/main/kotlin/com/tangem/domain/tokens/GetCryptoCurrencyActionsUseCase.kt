package com.tangem.domain.tokens

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Flow<TokenActionsState> {
        return flow {
            val actionStates = createTokenActionsState(userWalletId, cryptoCurrencyStatus)
            emit(actionStates)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(userWalletId, cryptoCurrencyStatus),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Buy Send Receive Sell Swap]
     */
    private suspend fun createListOfActions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(true))
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
        if (cryptoCurrencyStatus.value.amount.isNullOrZero()) {
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
}