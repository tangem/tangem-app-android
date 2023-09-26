package com.tangem.domain.tokens

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
            states = createListOfActions(userWalletId, cryptoCurrencyStatus.currency),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Buy Send Receive Sell Swap]
     */
    private suspend fun createListOfActions(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): List<TokenActionsState.ActionState> {
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
        activeList.add(TokenActionsState.ActionState.Send(true))
        // receive
        activeList.add(TokenActionsState.ActionState.Receive(true))

        // swap
        if (marketCryptoCurrencyRepository.isExchangeable(userWalletId, cryptoCurrency.id) &&
            !isCustomToken(cryptoCurrency)
        ) {
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

        return activeList + disabledList
    }

    private fun isCustomToken(currency: CryptoCurrency): Boolean {
        return currency is CryptoCurrency.Token && currency.isCustom
    }
}
